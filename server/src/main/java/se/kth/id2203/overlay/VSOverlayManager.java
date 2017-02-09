/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.overlay;

import com.larskroll.common.J6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.events.Booted;
import se.kth.id2203.bootstrapping.events.GetInitialAssignments;
import se.kth.id2203.bootstrapping.events.InitialAssignments;
import se.kth.id2203.bootstrapping.ports.Bootstrapping;
import se.kth.id2203.kvstore.events.ReplicationInit;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.events.OverlayInit;
import se.kth.id2203.overlay.ports.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.Collection;
import java.util.Set;

/**
 * The V(ery)S(imple)OverlayManager.
 * <p>
 * Keeps all nodes in a single partition in one replication group.
 * <p>
 * Note: This implementation does not fulfill the project task. You have to
 * support multiple partitions!
 * <p>
 * Handles routing and communication with bootstrap-server.
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class VSOverlayManager extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);
    //******* Ports ******
    protected final Negative<Routing> route = provides(Routing.class);
    protected final Positive<Bootstrapping> boot = requires(Bootstrapping.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<KVPort> kvPort = requires(KVPort.class);
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private LookupTable lut = null;
    private Component kvService;
    //******* Handlers ******

    public VSOverlayManager(OverlayInit overlayInit) {
        this.kvService = overlayInit.kvService;
    }

    /**
     * Bootstrap server requests to get initial assignment to partitions
     */
    protected final Handler<GetInitialAssignments> initialAssignmentHandler = new Handler<GetInitialAssignments>() {

        @Override
        public void handle(GetInitialAssignments event) {
            LOG.info("Generating LookupTable...");
            LookupTable lut = LookupTable.generate(event.nodes, event.replicationDegree, event.keySpace);
            LOG.debug("Generated assignments:\n{}", lut);
            trigger(new InitialAssignments(lut), boot);
        }
    };
    /**
     * Bootstrap client informs that bootup completed, event included the partition assignment
     */
    protected final Handler<Booted> bootHandler = new Handler<Booted>() {

        @Override
        public void handle(Booted event) {
            if (event.assignment instanceof LookupTable) {
                LOG.info("Got NodeAssignment, overlay ready.");
                lut = (LookupTable) event.assignment;
                int key = lut.reverseLookup(self);
                Collection<NetAddress> replicationGroup = lut.lookup(key);
                trigger(new ReplicationInit((Set) replicationGroup), kvPort);
            } else {
                LOG.error("Got invalid NodeAssignment type. Expected: LookupTable; Got: {}", event.assignment.getClass());
            }
        }
    };

    /**
     * Some operation for the key-value store to be routed. Retrieve the set of servers for the partition and route
     * message to a randomly selected server
     */
    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>() {

        @Override
        public void handle(RouteMsg content, Message context) {
            Collection<NetAddress> partition = lut.lookup(content.key);
            NetAddress target = J6.randomElement(partition);
            LOG.info("Forwarding message for key {} to {}", content.key, target);
            trigger(new Message(context.getSource(), target, content.msg), net);
        }
    };

    /**
     * Locally routed message
     */
    protected final Handler<RouteMsg> localRouteHandler = new Handler<RouteMsg>() {

        @Override
        public void handle(RouteMsg event) {
            Collection<NetAddress> partition = lut.lookup(event.key);
            NetAddress target = J6.randomElement(partition);
            LOG.info("Routing message for key {} to {}", event.key, target);
            trigger(new Message(self, target, event.msg), net);
        }
    };

    /**
     * Client wants to connect, either reject or respond with Ack that connect was accepted.
     */
    protected final ClassMatchedHandler<Connect, Message> connectHandler = new ClassMatchedHandler<Connect, Message>() {

        @Override
        public void handle(Connect content, Message context) {
            if (lut != null) {
                LOG.debug("Accepting connection request from {}", context.getSource());
                int size = lut.getNodes().size();
                trigger(new Message(self, context.getSource(), content.ack(size)), net);
            } else {
                LOG.info("Rejecting connection request from {}, as system is not ready, yet.", context.getSource());
            }
        }
    };


    /**
     * Kompics "instance initializer", subscribe handlers to ports.
     */ {
        subscribe(initialAssignmentHandler, boot);
        subscribe(bootHandler, boot);
        subscribe(routeHandler, net);
        subscribe(localRouteHandler, route);
        subscribe(connectHandler, net);
    }
}
