package se.kth.id2203.simulation.scenario.lin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.kvstore.events.KVServiceTimeout;
import se.kth.id2203.kvstore.events.ReplicationInit;
import se.kth.id2203.kvstore.events.RouteOperation;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.overlay.PID;
import se.kth.id2203.overlay.manager.ports.Routing;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.vsync.events.*;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ServiceComponent that handles the actual operation-requests from clients and return results.
 * ---- Scenario version that will put certain values into resultMap----
 *
 * @author Kim Hammar
 */
public class LinScenarioKVService extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<VSyncPort> vSyncPort = requires(VSyncPort.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Negative<KVPort> kvPort = provides(KVPort.class);
    /* Fields */
    private final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    private PID selfPid;
    private HashMap<Integer, String> keyValues = new HashMap<>();
    private long timestamp;
    private View replicationGroup;
    private boolean blocked;
    private Queue<RouteOperation> operationQueue = new LinkedList<>();
    private Queue<RouteOperation> pendingWrites = new LinkedList<>();
    private UUID timeoutId;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    /**
     * Setup timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            long timeout = config().getValue("id2203.project.kvservice.timeout", Long.class);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new KVServiceTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    /**
     * Flush operation queue if not blocked
     */
    protected final Handler<KVServiceTimeout> timeoutHandler = new Handler<KVServiceTimeout>() {
        @Override
        public void handle(KVServiceTimeout event) {
            if(operationQueue.size() > 0 && !blocked && replicationGroup != null){
                while(operationQueue.size() > 0){
                    trigger(new Message(selfPid.netAddress, replicationGroup.leader.netAddress, operationQueue.poll()), net);
                }
            }
        }
    };

    /**
     * Received Operation routed from overlay.
     * Route it again within the replication-group to the leader.
     */
    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {
        @Override
        public void handle(Operation content, Message context) {
            Queue trace = res.get("trace", ConcurrentLinkedQueue.class);
            trace.add(content);
            LOG.info("Got operation {}, routing it to leader..", content);
            RouteOperation routeOperation = new RouteOperation(content, context.getSource());
            if (!blocked && replicationGroup != null) {
                trigger(new Message(selfPid.netAddress, replicationGroup.leader.netAddress, routeOperation), net);
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
            if (replicationGroup.leader.equals(selfPid)) {
                timestamp++;
                LOG.debug("Leader received operation");
                StateUpdate update;
                if (!blocked) {
                    switch (routeOperation.operation.operationCode) {
                        case GET:
                            String val = keyValues.get(routeOperation.operation.key.hashCode());
                            if (val == null)
                                val = "not found";
                            trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, val)), net);
                            break;
                        case PUT:
                            keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                            update = new StateUpdate(ImmutableMap.copyOf(keyValues), timestamp);
                            trigger(new VS_Broadcast(update, replicationGroup.id), vSyncPort);
                            routeOperation.id = update.id;
                            pendingWrites.add(routeOperation);
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
                                pendingWrites.add(routeOperation);
                            } else {
                                trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id,
                                        OpResponse.Code.OK, "Reference value does not match new value")), net);
                            }
                            break;
                        default:
                            trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.NOT_IMPLEMENTED)), net);
                            break;
                    }
                } else {
                    operationQueue.add(routeOperation);
                }
            } else {
                trigger(new Message(selfPid.netAddress, replicationGroup.leader.netAddress, routeOperation), net);
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
            selfPid = replicationInit.self;
            trigger(new VSyncInit(ImmutableSet.copyOf(replicationInit.nodes), selfPid, new StateUpdate(ImmutableMap.copyOf(keyValues), timestamp)), vSyncPort);
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
            res.put(selfPid.netAddress.getIp().getHostAddress(), keyValues);
        }
    };

    /**
     * Write complete, respond to client
     */
    protected final ClassMatchedHandler<WriteComplete, VS_Deliver> writeCompleteHandler = new ClassMatchedHandler<WriteComplete, VS_Deliver>() {
        @Override
        public void handle(WriteComplete writeComplete, VS_Deliver vs_deliver) {
            Iterator<RouteOperation> i = pendingWrites.iterator();
            while (i.hasNext()) {
                RouteOperation routeOperation = i.next();
                if(writeComplete.id.equals(routeOperation.id)){
                    LOG.debug("Write complete from Vsync layer, delivering to the client");
                    trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, "Write successful")), net);
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
     */
    {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(routedOpHandler, net);
        subscribe(opHandler, net);
        subscribe(viewHandler, vSyncPort);
        subscribe(blockHandler, vSyncPort);
        subscribe(writeCompleteHandler, vSyncPort);
        subscribe(stateUpdateHandler, vSyncPort);
        subscribe(replicationInitHandler, kvPort);
    }
}