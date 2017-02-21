package se.kth.id2203.simulation.scenario.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import se.kth.id2203.gms.events.View;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.kvstore.events.ReplicationInit;
import se.kth.id2203.kvstore.events.RouteOperation;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.ports.Routing;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.vsync.events.Block;
import se.kth.id2203.vsync.events.BlockOk;
import se.kth.id2203.vsync.events.StateUpdate;
import se.kth.id2203.vsync.events.VS_Broadcast;
import se.kth.id2203.vsync.events.VS_Deliver;
import se.kth.id2203.vsync.events.VSyncInit;
import se.kth.id2203.vsync.events.WriteComplete;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

public class ViewScenarioKVService extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<VSyncPort> vSyncPort = requires(VSyncPort.class);
    protected final Negative<KVPort> kvPort = provides(KVPort.class);
    /* Fields */
    private final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private HashMap<Integer, String> keyValues = new HashMap<>();
    private long timestamp;
    private View replicationGroup;
    private boolean blocked;
    private Queue<RouteOperation> operationQueue;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    

    /**
     * Received Operation routed from overlay.
     * Route it again within the replication-group to the leader.
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
                timestamp++;
                LOG.debug("Leader received operation");
                StateUpdate update;
                if (!blocked) {
                    switch (routeOperation.operation.operationCode) {
                        case GET:
                            String val = keyValues.get(routeOperation.operation.key.hashCode());
                            if (val == null)
                                val = "not found";
                            trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, val)), net);
                            break;
                        case PUT:
                            keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                            update = new StateUpdate(ImmutableMap.copyOf(keyValues), timestamp);
                            trigger(new VS_Broadcast(update, replicationGroup.id), vSyncPort);
                            routeOperation.id = update.id;
                            operationQueue.add(routeOperation);
                            break;
                        case CAS:
                        	String res = keyValues.get(routeOperation.operation.key.hashCode());
                        	LOG.warn("Key {} | Reference value {} | New value {} | Result from get {}",
                        			routeOperation.operation.key.hashCode(),
                        			routeOperation.operation.referenceValue,
                        			routeOperation.operation.value,
                        			res);
                        	if(res !=null && res.equals(routeOperation.operation.referenceValue)){
                                keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                                update = new StateUpdate(ImmutableMap.copyOf(keyValues), timestamp);
                                trigger(new VS_Broadcast(update, replicationGroup.id), vSyncPort);
                                routeOperation.id = update.id;
                                operationQueue.add(routeOperation);	
                        	} else {
                        	  trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, 
                        			  OpResponse.Code.OK, "Reference value does not match new value")), net);
                        	}
                        	break;
                        default:
                            trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.NOT_IMPLEMENTED)), net);
                            break;
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
            HashMap<String,Object> result = new HashMap<>();
            result.put("members", convertFromNetAddr(view.members));
            result.put("id", view.id);
            result.put("leader", view.leader.getIp().getHostAddress());
            res.put(self.getIp().getHostAddress(),result);
            blocked = false;
            replicationGroup = view;
        }
    };
    
    private Set<String> convertFromNetAddr(Set<NetAddress> addresses){
    	Set<String> result = new HashSet<>();
    	for (NetAddress addr : addresses) {
			result.add(addr.getIp().getHostAddress());
		}
    	return result;
    }

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
            keyValues = new HashMap<>();
            if (stateUpdate != null)
                keyValues.putAll(stateUpdate.keyValues);
            printStore();
        }
    };

    /**
     * Write complete, respond to client
     */
    protected final ClassMatchedHandler<WriteComplete, VS_Deliver> writeCompleteHandler = new ClassMatchedHandler<WriteComplete, VS_Deliver>() {
        @Override
        public void handle(WriteComplete writeComplete, VS_Deliver vs_deliver) {
        	Iterator<RouteOperation> i = operationQueue.iterator();
        	while (i.hasNext()) {
        	   RouteOperation routeOperation = i.next();
        	   if(writeComplete.id.equals(routeOperation.id)){
                  	LOG.debug("Write complete from Vsync layer, delivering to the client");
                  	trigger(new Message(self, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, "Write successful")), net);
                  	i.remove();
                  } 
        	}          
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
        subscribe(viewHandler, vSyncPort);
        subscribe(blockHandler, vSyncPort);
        subscribe(writeCompleteHandler, vSyncPort);
        subscribe(stateUpdateHandler, vSyncPort);
        subscribe(replicationInitHandler, kvPort);
    }

}
