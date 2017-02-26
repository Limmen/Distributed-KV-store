package se.kth.id2203.overlay.service;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.events.Boot;
import se.kth.id2203.bootstrapping.events.CheckIn;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.epfd.events.EPFDInit;
import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.gms.events.GMSJoin;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.kvstore.events.Handover;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.PID;
import se.kth.id2203.overlay.lookuptable.LookupTable;
import se.kth.id2203.overlay.service.events.GlobalView;
import se.kth.id2203.overlay.service.events.Gossip;
import se.kth.id2203.overlay.service.events.JoinPending;
import se.kth.id2203.overlay.service.events.VSOverlayServiceInit;
import se.kth.id2203.overlay.service.ports.OverlayServicePort;
import se.kth.id2203.overlay.service.timeouts.OverlayServiceTimeout;
import se.kth.id2203.vsync.events.StateTransfer;
import se.kth.id2203.vsync.events.VS_Deliver;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.*;

/**
 * Component that maintains the lookup-table when churn happens.
 *
 * @author Kim Hammar on 2017-02-22.
 */
public class VSOverlayService extends ComponentDefinition {

    /* Ports */
    protected final Negative<OverlayServicePort> overlayServicePort = provides(OverlayServicePort.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<BEBPort> broadcastPort = requires(BEBPort.class);
    protected final Positive<VSyncPort> vSyncPort = requires(VSyncPort.class);
    protected final Positive<KVPort> kvPort = requires(KVPort.class);
    /* Fields */
    final static Logger LOG = LoggerFactory.getLogger(VSOverlayService.class);
    private LookupTable lookupTable;
    private PID selfPid;
    private Set<PID> successorAlive = new HashSet<>();
    private View view;
    private int partition;
    private UUID timeoutId;
    private final int replicationDegree = config().getValue("id2203.project.replicationDegree", Integer.class);
    final int keySpace = config().getValue("id2203.project.keySpace", Integer.class);
    private final Map<Integer, Long> latestViews = new HashMap();
    private Set<NetAddress> pendingJoins = new HashSet<>();
    private State state;
    private HashMap<Integer, String> keyValues = new HashMap<>();
    private int newJoins = 0;

    /**
     * Setup timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            state = State.WAITING;
            long timeout = config().getValue("id2203.project.overlayservice.timeout", Long.class);
            LOG.info("Starting VSOverlayService with timeout {}", timeout);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new OverlayServiceTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
            LOG.debug("Timeout set");
        }
    };

    /**
     * Initialize VSOverlayService with a global view
     */
    protected final Handler<VSOverlayServiceInit> initHandler = new Handler<VSOverlayServiceInit>() {
        @Override
        public void handle(VSOverlayServiceInit init) {
            state = State.BOOTED;
            LOG.debug("VSOverlayService initialized");
            lookupTable = init.lookupTable;
            selfPid = init.pid;
            partition = lookupTable.reverseLookup(selfPid);
            monitorSuccessor();
        }
    };

    private void monitorSuccessor() {
        int successor = lookupTable.succ(partition);
        LOG.debug("Partition {} now monitoring successor-partition {}", partition, successor);
        successorAlive = new HashSet<>((Set) lookupTable.lookup(successor));
        trigger(new EPFDInit(new HashSet<PID>((Set) successorAlive), selfPid), epfdPort);
    }

    /**
     * Trigger Gossip periodically if we are leader. Create new partition if there is enough pending joins
     */
    protected final Handler<OverlayServiceTimeout> timeoutHandler = new Handler<OverlayServiceTimeout>() {
        @Override
        public void handle(OverlayServiceTimeout event) {
            if (state == State.LEADER) {
                if (successorAlive.size() < replicationDegree) {
                    LOG.info("OVerlay detected crash of successor partition {}.. gossiping", lookupTable.succ(partition));
                    trigger(new BEB_Broadcast(new Gossip(lookupTable.succ(partition), null, true), lookupTable.getNodesSet(), selfPid), broadcastPort);
                    lookupTable.removePartition(lookupTable.succ(partition));
                    monitorSuccessor();
                }
                if (view != null && view.leader.equals(selfPid)) {
                    trigger(new BEB_Broadcast(new Gossip(partition, view, false), lookupTable.getNodesSet(), selfPid), broadcastPort);
                }
                if (pendingJoins.size() >= replicationDegree && lookupTable.getEdgeKey() == partition) {
                    LOG.info("VSOverlayService received enough join-requests to boot a new partition");
                    int createPartitionId = lookupTable.getEdgeKey() + keySpace;
                    Iterator iterator = pendingJoins.iterator();
                    for (int i = 0; i < replicationDegree * 2 - 1; i++) {
                        if(iterator.hasNext()){
                            NetAddress node = (NetAddress) iterator.next();
                            PID pid = new PID(node, lookupTable.getNewPid());
                            lookupTable.putNode(createPartitionId, pid);
                            iterator.remove();
                        }
                    }
                    iterator = lookupTable.getPartition(createPartitionId).iterator();
                    while (iterator.hasNext()) {
                        PID node = (PID) iterator.next();
                        HashMap<Integer, String> handoverStore = handover(createPartitionId);
                        LOG.debug("HandoverStore: " + handoverStore);
                        LOG.debug("Kept store: " + keyValues);
                        monitorSuccessor();
                        trigger(new Message(selfPid.netAddress, node.netAddress, new Boot(lookupTable, handoverStore)), net);
                    }
                    pendingJoins = new HashSet<>();
                }
                if (lookupTable.getEdgeKey() != partition && pendingJoins.size() > 0) {
                    //We are not the "edge" in the ring, overlay structure might have changed while the joins were pending
                    //forward the join request to the edge partition instead.
                    LOG.debug("Forwarding CheckIn to right partition");
                    Set partitionSet = (Set) lookupTable.lookup(lookupTable.getEdgeKey());
                    PID targetLeader = (PID) Collections.max(partitionSet);
                    for (NetAddress joiningNode : pendingJoins) {
                        trigger(new Message(selfPid.netAddress, targetLeader.netAddress, new CheckIn(joiningNode)), net);
                    }
                }
            }
        }
    };

    /**
     * Received gossip, update our lookup-table accordingly. 2 types of gossip: partition crash and partition-view change
     */
    protected final ClassMatchedHandler<Gossip, BEB_Deliver> deliverHandler = new ClassMatchedHandler<Gossip, BEB_Deliver>() {
        @Override
        public void handle(Gossip gossip, BEB_Deliver beb_deliver) {
            if (gossip.partitionId == partition && !gossip.view.members.contains(selfPid)) {
                LOG.warn("Ring-partition that have healed, crash to make room for other partition");
                Kompics.shutdown();
            }
            if (gossip.crashed) {
                LOG.info("Received gossip that partition {} crashed", gossip.partitionId);
                lookupTable.removePartition(gossip.partitionId);
                if (gossip.partitionId == lookupTable.succ(partition)) {
                    monitorSuccessor();
                }
                trigger(new GlobalView(lookupTable), overlayServicePort);
                printTable();
                return;
            }
            Long latestView = latestViews.get(gossip.partitionId);
            if (beb_deliver.source.equals(gossip.view.leader) && (latestView == null || gossip.view.id > latestView)) {
                lookupTable.putPartition(gossip.partitionId, gossip.view.members);
                trigger(new GlobalView(lookupTable), overlayServicePort);
                printTable();
                if (gossip.partitionId == lookupTable.succ(partition)) {
                    monitorSuccessor();
                }
                latestViews.put(gossip.partitionId, gossip.view.id);
                trigger(new BEB_Broadcast(gossip, lookupTable.getNodesSet(), selfPid), broadcastPort);
            }
        }
    };

    /**
     * Received new view from VSyncService
     */
    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View newView) {
            LOG.debug("VSOverlayService recieved new view from VSyncService");
            newJoins = 0;
            view = newView;
            latestViews.put(partition, view.id);
            if (view.leader.equals(selfPid))
                state = State.LEADER;
            else
                state = State.BACKUP;
            if(state == State.LEADER && lookupTable.succ(partition) != partition){
                handover(lookupTable.succ(partition));
            }
            lookupTable.putPartition(partition, view.members);
            trigger(new GlobalView(lookupTable), overlayServicePort);
            printTable();
        }
    };

    /**
     * CheckIn request from a new server that wants to join the cluster.
     * Add to our partition if we are leader and there is some empty slot. Otherwise a new partition need to be
     * created when there are enough join-requests.
     */
    protected final ClassMatchedHandler<CheckIn, Message> joinHandler = new ClassMatchedHandler<CheckIn, Message>() {
        @Override
        public void handle(CheckIn checkIn, Message message) {
            if (state == State.LEADER) {
                LOG.debug("OVerlayService leader received Join request");
                int targetPartition = lookupTable.freePartition(replicationDegree);
                if (targetPartition != partition) {
                    Set partitionSet = (Set) lookupTable.lookup(targetPartition);
                    PID targetLeader = (PID) Collections.max(partitionSet);
                    LOG.debug("Forwarding CheckIn to right partition");
                    trigger(new Message(selfPid.netAddress, targetLeader.netAddress, checkIn), net);
                } else {
                    //Handle join
                    if (view.members.size()+newJoins < replicationDegree * 2 - 1) {
                        PID node = new PID(checkIn.netaddress, lookupTable.getNewPid());
                        LookupTable copy = new LookupTable(lookupTable);
                        copy.putNode(partition, node);
                        Boot boot = new Boot(copy, keyValues);
                        trigger(new Message(selfPid.netAddress, checkIn.netaddress, boot), net);
                        trigger(new GMSJoin(node), vSyncPort);
                        newJoins++;
                    } else {
                        //All partitions are full, need to create a new one.
                        pendingJoins.add(checkIn.netaddress);
                        //Inform joining server that the join is pending until enough servers for a new partition have joined
                        trigger(new Message(selfPid.netAddress, checkIn.netaddress, new JoinPending()), net);
                    }
                }
            }
            //Forward to leader otherwise
            else if (state == State.BACKUP) {
                trigger(new Message(selfPid.netAddress, view.leader.netAddress, checkIn), net);
            }
        }
    };

    private HashMap<Integer, String> handover(int createPartitionKey){
        HashMap<Integer, String> store1 = new HashMap<>();
        HashMap<Integer, String> store2 = new HashMap<>();
        for (int key : keyValues.keySet()) {
            if(lookupTable.lookupPartitionKey(key) == createPartitionKey)
                store2.put(key, keyValues.get(key));
            else if(lookupTable.lookupPartitionKey(key) == partition)
                store1.put(key, keyValues.get(key));
        }
        if(!keyValues.equals(store1)){
            keyValues = store1;
            trigger(new Handover(ImmutableMap.copyOf(keyValues)), kvPort);
        }
        return store2;
    }

    /**
     * EPFD suspected some process in the sucessor partition
     */
    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {
        @Override
        public void handle(Suspect event) {
            successorAlive.remove(event.suspected);
        }
    };

    /**
     * EPFD restored suspicion of some process in the successor partition
     */
    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {
        @Override
        public void handle(Restore event) {
            successorAlive.add(event.restored);
        }
    };

    /**
     * Received state update from VSyncService
     */
    protected final ClassMatchedHandler<StateTransfer, VS_Deliver> stateUpdateHandler = new ClassMatchedHandler<StateTransfer, VS_Deliver>() {
        @Override
        public void handle(StateTransfer stateTransfer, VS_Deliver vs_deliver) {
            keyValues = new HashMap<>();
            if (stateTransfer != null)
                keyValues.putAll(stateTransfer.keyValues);
        }
    };

    private void printTable() {
        LOG.info("LookupTable updated: {}", lookupTable);
    }

    {
        subscribe(stateUpdateHandler, vSyncPort);
        subscribe(suspectHandler, epfdPort);
        subscribe(restoreHandler, epfdPort);
        subscribe(initHandler, overlayServicePort);
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(deliverHandler, broadcastPort);
        subscribe(viewHandler, vSyncPort);
        subscribe(joinHandler, net);
    }

    static enum State {
        LEADER,
        BACKUP,
        WAITING,
        BOOTED
    }
}
