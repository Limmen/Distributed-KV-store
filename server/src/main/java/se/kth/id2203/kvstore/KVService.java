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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.kvstore.events.ReplicationInit;
import se.kth.id2203.kvstore.events.RouteOperation;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.ports.Routing;
import se.kth.id2203.vsync.events.*;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ServiceComponent that handles the actual operation-requests from clients and return results.
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVService extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<VSyncPort> vSyncPort = requires(VSyncPort.class);
    protected final Negative<KVPort> kvPort = provides(KVPort.class);
    /* Fields */
    final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    final HashMap<Integer, String> keyValues = new HashMap<>();
    private long timestamp;
    private View replicationGroup;
    private boolean blocked;
    private Queue<RouteOperation> operationQueue;


    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            keyValues.put("1".hashCode(), "first");
        }
    };

    /**
     *
     */
    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {
        @Override
        public void handle(Operation content, Message context) {
            LOG.info("Got operation {}, routing it to leader..", content);
            RouteOperation routeOperation = new RouteOperation(content, context.getSource());
            if (!blocked) {
                trigger(new Message(self, replicationGroup.leader, routeOperation), net);
            } else {
                operationQueue.add(routeOperation);
            }
        }

    };

    /**
     * Received operation routed through the replication-group, if this process is leader, handle operation and return
     * result to client.
     */
    protected final ClassMatchedHandler<RouteOperation, Message> routedOpHandler = new ClassMatchedHandler<RouteOperation, Message>() {
        @Override
        public void handle(RouteOperation routeOperation, Message message) {
            if (replicationGroup.leader.sameHostAs(self)) {
                LOG.debug("Leader received operation");
                if (!blocked) {
                    switch (routeOperation.operation.operationCode) {
                        case GET:
                            trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, keyValues.get(routeOperation.operation.key.hashCode()))), net);
                        case PUT:
                            keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                            trigger(new VS_Broadcast(new StateUpdate(ImmutableMap.copyOf(keyValues),timestamp), replicationGroup.id), vSyncPort);
                            trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, "Write successful")), net);
                        default:
                            trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.NOT_IMPLEMENTED)), net);
                    }
                } else {
                    operationQueue.add(routeOperation);
                }
            } else {
                trigger(new Message(self, replicationGroup.leader, routeOperation), net);
            }
        }
    };

    /**
     * Received new view from VSyncService
     */
    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            LOG.debug("KVService recieved new view from VSyncService");
            blocked = false;
            replicationGroup = view;
        }
    };

    /**
     * Joined new replication-group
     */
    protected final Handler<ReplicationInit> replicationInitHandler = new Handler<ReplicationInit>() {
        @Override
        public void handle(ReplicationInit replicationInit) {
            LOG.info("KVService initializes replication group");
            timestamp = 0;
            blocked = false;
            operationQueue = new LinkedList<>();
            trigger(new VSyncInit(ImmutableSet.copyOf(replicationInit.nodes), new StateUpdate(ImmutableMap.copyOf(keyValues), timestamp)), vSyncPort);
        }
    };

    /**
     * Received state update from VSyncService
     */
    protected final ClassMatchedHandler<StateUpdate, VS_Deliver> stateUpdateHandler = new ClassMatchedHandler<StateUpdate, VS_Deliver>() {
        @Override
        public void handle(StateUpdate stateUpdate, VS_Deliver vs_deliver) {
            LOG.debug("KVService received state update from VSyncService");
            keyValues.clear();
            if (stateUpdate != null)
                keyValues.putAll(stateUpdate.keyValues);
            timestamp++;
            printStore();
        }
    };

    /**
     * VSyncLayer ask us to block requests while installing new view
     */
    protected final Handler<Block> blockHandler = new Handler<Block>() {
        @Override
        public void handle(Block block) {
            LOG.debug("KVService received block request from VSyncService");
            blocked = true;
            trigger(new BlockOk(), vSyncPort);
        }
    };

    private void printStore() {
        System.out.println("--------------------------------------");
        System.out.println("Store:");
        for (int key : keyValues.keySet()) {
            System.out.println("key: " + key + " | value: " + keyValues.get(key));
        }
        System.out.println("--------------------------------------");
    }

    /**
     * Kompics "instance initializer", subscribe handlers to ports.
     */ {
        subscribe(routedOpHandler, net);
        subscribe(opHandler, net);
        subscribe(startHandler, control);
        subscribe(viewHandler, vSyncPort);
        subscribe(blockHandler, vSyncPort);
        subscribe(stateUpdateHandler, vSyncPort);
        subscribe(replicationInitHandler, kvPort);
    }

}
