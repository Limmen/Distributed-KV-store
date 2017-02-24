package se.kth.id2203.simulation.scenario.view;


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


public class ViewScenarioKVService extends ComponentDefinition {

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
    private Queue<RouteOperation> pendingOperations = new LinkedList<>();
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
            if (operationQueue.size() > 0 && !blocked && replicationGroup != null) {
                while (operationQueue.size() > 0) {
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
                if (!blocked) {
                    switch (routeOperation.operation.operationCode) {
                        case GET:
                            sendOp(routeOperation);
                            break;
                        case PUT:
                            keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                            sendOp(routeOperation);
                            break;
                        case CAS:
                            routeOperation.oldValue = keyValues.get(routeOperation.operation.key.hashCode());
                            if (routeOperation.oldValue != null && routeOperation.oldValue.equals(routeOperation.operation.referenceValue))
                                keyValues.put(routeOperation.operation.key.hashCode(), routeOperation.operation.value);
                            sendOp(routeOperation);
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

    private void sendOp(RouteOperation routeOperation) {
        StateTransfer snapshot = new StateTransfer(ImmutableMap.copyOf(keyValues), timestamp);
        trigger(new VS_Broadcast(snapshot, replicationGroup.id), vSyncPort);
        routeOperation.id = snapshot.id;
        pendingOperations.add(routeOperation);
    }


    /**
     * Received new view from VSyncService
     */
    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            LOG.debug("KVService recieved new view from VSyncService: " + view);
            HashMap<String,Object> result = new HashMap<>();
            result.put("members", convertFromNetAddr(view.members));
            result.put("id", view.id);
            result.put("leader", view.leader.netAddress.getIp().getHostAddress());
            ArrayList<HashMap<String,Object>> r = res.get(selfPid.netAddress.getIp().getHostAddress(), ArrayList.class);
            r.add(result);
            blocked = false;
            replicationGroup = view;
        }
    };

    private Set<String> convertFromNetAddr(Set<PID> addresses){
        Set<String> result = new HashSet<>();
        for (PID pid : addresses) {
            result.add(pid.netAddress.getIp().getHostAddress());
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
            selfPid = replicationInit.self;
            res.put(selfPid.netAddress.getIp().getHostAddress(),new ArrayList<>());
            trigger(new VSyncInit(ImmutableSet.copyOf(replicationInit.nodes), selfPid, new StateTransfer(ImmutableMap.copyOf(keyValues), timestamp)), vSyncPort);
        }
    };

    /**
     * Received state update from VSyncService
     */
    protected final ClassMatchedHandler<StateTransfer, VS_Deliver> stateUpdateHandler = new ClassMatchedHandler<StateTransfer, VS_Deliver>() {
        @Override
        public void handle(StateTransfer stateTransfer, VS_Deliver vs_deliver) {
            LOG.debug("KVService received state update from VSyncService");
            keyValues = new HashMap<>();
            if (stateTransfer != null)
                keyValues.putAll(stateTransfer.keyValues);
            printStore();
        }
    };

    /**
     * Operation complete, respond to client
     */
    protected final ClassMatchedHandler<OperationComplete, VS_Deliver> writeCompleteHandler = new ClassMatchedHandler<OperationComplete, VS_Deliver>() {
        @Override
        public void handle(OperationComplete operationComplete, VS_Deliver vs_deliver) {
            Iterator<RouteOperation> i = pendingOperations.iterator();
            while (i.hasNext()) {
                RouteOperation routeOperation = i.next();
                if (operationComplete.id.equals(routeOperation.id)) {
                    LOG.debug("Operation complete from Vsync layer, delivering to the client");
                    switch (routeOperation.operation.operationCode) {
                        case GET:
                            String val = keyValues.get(routeOperation.operation.key.hashCode());
                            if (val == null)
                                val = "not found";
                            trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, val)), net);
                            break;
                        case PUT:
                            trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, "Write successful")), net);
                            break;
                        case CAS:
                            if (routeOperation.oldValue == null) {
                                routeOperation.oldValue = "not found";
                            }
                            trigger(new Message(selfPid.netAddress, routeOperation.client, new OpResponse(routeOperation.operation.id, OpResponse.Code.OK, routeOperation.oldValue)), net);
                            break;
                    }
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
