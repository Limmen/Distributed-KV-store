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
package se.kth.id2203.kvstore;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.kvstore.events.ReplicationInit;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.ports.Routing;
import se.kth.id2203.vsync.events.VSyncInit;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;

/**
 * ServiceComponent that handles the actual operation-requests from clients and return results.
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVService extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<VSyncPort> vSyncPort = requires(VSyncPort.class);
    protected final Negative<KVPort> kvPort = provides(KVPort.class);
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    final HashMap<Integer, String> keyValues = new HashMap<>();
    private View replicationGroup;
    //******* Handlers ******

    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            keyValues.put("1".hashCode(), "first");
        }
    };

    /**
     * Operation request received, perform operation and return result.
     */
    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {
        @Override
        public void handle(Operation content, Message context) {
            LOG.info("Got operation {}! Now implement me please :)", content);
            switch (content.operationCode) {
                case GET:
                    trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK, keyValues.get(content.key.hashCode()))), net);
                default:
                    trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_IMPLEMENTED)), net);
            }
        }

    };

    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            LOG.debug("KVService recieved new view from VSyncService");
            replicationGroup = view;
        }
    };

    protected final Handler<ReplicationInit> replicationInitHandler = new Handler<ReplicationInit>() {
        @Override
        public void handle(ReplicationInit replicationInit) {
            LOG.info("KVService initializes replication group");
            trigger(new VSyncInit(ImmutableSet.copyOf(replicationInit.nodes)), vSyncPort);
        }
    };

    /**
     * Kompics "instance initializer", subscribe handlers to ports.
     */
    {
        subscribe(opHandler, net);
        subscribe(startHandler, control);
        subscribe(viewHandler, vSyncPort);
        subscribe(replicationInitHandler, kvPort);
    }

}
