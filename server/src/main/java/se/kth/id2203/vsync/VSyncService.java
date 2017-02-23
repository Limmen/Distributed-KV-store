package se.kth.id2203.vsync;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.gms.events.GMSInit;
import se.kth.id2203.gms.events.GMSJoin;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.gms.ports.GMSPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.PID;
import se.kth.id2203.vsync.events.*;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.kth.id2203.vsync.timeout.VSyncTimeout;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.*;

/**
 * Virtual Synchrony Service. Gives the View-Inclusion property as well as the properties from underlying GMS and BEB
 * components.
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class VSyncService extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Negative<VSyncPort> vSyncPort = provides(VSyncPort.class);
    protected final Positive<GMSPort> gmsPort = requires(GMSPort.class);
    protected final Positive<BEBPort> broadcastPort = requires(BEBPort.class);
    /* Fields */
    private final static Logger LOG = LoggerFactory.getLogger(VSyncService.class);
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private PID selfPid;
    private View currentView;
    private long viewId = 0;
    private boolean flushing;
    private boolean blocked;
    private StateUpdate latestUpdate;
    private Queue<View> pendingViews = new LinkedList<>();
    private Set<PID> flushes = new HashSet<>();
    private UUID timeoutId;
    private Set<PID> acks = new HashSet<>();
    private Queue<StateUpdate> pendingUpdates;
    private StateUpdate pendingUpdate;

    /**
     * Initialize timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            long timeout = config().getValue("id2203.project.vsync.timeout", Long.class);
            selfPid = new PID(self, 0);
            pendingUpdates = new LinkedList<>();
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new VSyncTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    /**
     * Initialize VSync service for a set of nodes
     */
    protected final Handler<VSyncInit> vSyncInitHandler = new Handler<VSyncInit>() {
        @Override
        public void handle(VSyncInit vSyncInit) {
            LOG.debug("VSyncService initialized");
            flushing = false;
            blocked = true;
            latestUpdate = vSyncInit.stateUpdate;
            pendingViews = new LinkedList<>();
            flushes = new HashSet<>();
            selfPid = vSyncInit.self;
            trigger(new GMSInit(ImmutableSet.copyOf(vSyncInit.nodes), selfPid), gmsPort);
            trigger(new Block(), vSyncPort);
        }
    };

    /**
     * Timeout, if we are leader of new view and have pending view, attempt to install new view by collecting acks.
     * Also if we are leader and we have pending updates, broadcast and collect ACK's before delivering to upper layer.
     */
    protected final Handler<VSyncTimeout> timeoutHandler = new Handler<VSyncTimeout>() {
        @Override
        public void handle(VSyncTimeout event) {
            if (pendingViews.size() > 0 && !flushing) {
                flushing = true;
                LOG.debug("Pending view change.. triggering Block request to KV-layer");
                if (!blocked)
                    trigger(new Block(), vSyncPort);
            }
            if (pendingViews.size() > 0 && flushing && blocked && pendingViews.peek().leader.equals(selfPid)) {
                Set<PID> notFlushed = new HashSet<>();
                for (PID member : pendingViews.peek().members) {
                    if (!flushes.contains(member))
                        notFlushed.add(member);
                }
                try {
                    if (notFlushed.size() > 0) {
                        LOG.debug("Missing {} flushes before new view can be installed, re-sending flushrequest..", notFlushed.size());
                        trigger(new BEB_Broadcast(new FlushReq(pendingViews.peek().id, viewId), notFlushed, selfPid), broadcastPort);
                    } else {
                        LOG.debug("Received all flushes, installing new view");
                        currentView = pendingViews.remove();
                        viewId = currentView.id;
                        trigger(new BEB_Broadcast(new ViewInstall(currentView, latestUpdate), currentView.members, selfPid), broadcastPort);
                    }
                } catch (Exception e) {
                    LOG.debug("Exception!");
                    e.printStackTrace();
                }
            }
            if (pendingUpdates.size() > 0 && pendingUpdate == null) {
                pendingUpdate = pendingUpdates.poll();
                acks = new HashSet<>();
            }
            if (currentView != null && currentView.leader.equals(selfPid) && pendingUpdate != null) {
                Set<PID> notAcked = new HashSet<>();
                for (PID member : currentView.members) {
                    if (!acks.contains(member))
                        notAcked.add(member);
                }
                if (notAcked.size() > 0) {
                    LOG.warn("Resending update to backups waiting for {} nodes ", notAcked.size());
                    trigger(new BEB_Broadcast(new VS_Deliver(pendingUpdate, selfPid, viewId), notAcked, selfPid), broadcastPort);
                } else {
                    trigger(new VS_Deliver(new WriteComplete(pendingUpdate.id), selfPid, viewId), vSyncPort);
                    latestUpdate = pendingUpdate;
                    pendingUpdate = null;
                }
            }
        }
    };

    /**
     * Received operation for the replication group from the network
     */
    protected final ClassMatchedHandler<VS_Broadcast, Message> netBroadcastHandler = new ClassMatchedHandler<VS_Broadcast, Message>() {
        @Override
        public void handle(VS_Broadcast vs_broadcast, Message message) {
            handleBroadcast(vs_broadcast);
        }
    };

    /**
     * Received operation for the replication group from the KVLayer above
     */
    protected final Handler<VS_Broadcast> broadcastHandler = new Handler<VS_Broadcast>() {
        @Override
        public void handle(VS_Broadcast vs_broadcast) {
            handleBroadcast(vs_broadcast);
        }
    };

    /**
     * Handle operation for the replication group. If we are leader, broadcast to all replicas, otherwise forward to
     * leader.
     *
     * @param vs_broadcast operation
     */
    private void handleBroadcast(VS_Broadcast vs_broadcast) {
        if (vs_broadcast.viewId == viewId) {
            if (currentView.leader.equals(selfPid)) {
                LOG.debug("VSyncService leader received request, sending broadcast");
                VS_Deliver vs_deliver = new VS_Deliver(vs_broadcast.payload, selfPid, viewId);
                pendingUpdates.add(vs_broadcast.payload);
                trigger(new BEB_Broadcast(vs_deliver, currentView.members, selfPid), broadcastPort);
            } else {
                LOG.debug("VSyncService member received request, forwarding to leader");
                trigger(new Message(selfPid.netAddress, currentView.leader.netAddress, vs_broadcast), net);
            }
        }
    }

    /**
     * Received operation from leader-broadcast, deliver to KVLayer.
     */
    protected final ClassMatchedHandler<VS_Deliver, BEB_Deliver> deliverHandler = new ClassMatchedHandler<VS_Deliver, BEB_Deliver>() {
        @Override
        public void handle(VS_Deliver vs_deliver, BEB_Deliver beb_deliver) {
            if (vs_deliver.viewId == viewId && vs_deliver.source.equals(currentView.leader)) {
                LOG.debug("Received StateUpdate from leader in view, delivering to application");
                latestUpdate = (StateUpdate) vs_deliver.payload;
                UpdateAcc acc = new UpdateAcc(latestUpdate.id, selfPid);
                LOG.warn("Sending acc to {} ", beb_deliver.source);
                trigger(new Message(selfPid.netAddress, beb_deliver.source.netAddress, acc), net);
                trigger(vs_deliver, vSyncPort);
            }
        }
    };

    /**
     * Received ACK for update from backup in the view
     */
    protected final ClassMatchedHandler<UpdateAcc, Message> accHandler = new ClassMatchedHandler<UpdateAcc, Message>() {

        @Override
        public void handle(UpdateAcc content, Message context) {
            LOG.warn("Received acc from {} ", context.getSource());
            acks.add(content.source);
        }
    };

    /**
     * Received view from GMS, if we are leader we will try to install it, otherwise wait for other leader to try to
     * install it.
     */
    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            if (view.leader.equals(selfPid)) {
                LOG.debug("Received view from GMS with itself as leader, trying to install it");
                pendingViews.add(view);
                flushes = new HashSet<>();
            } else {
                LOG.debug("Received view from GMS, adding it to pending list and waiting for leader to install it");
                pendingViews.add(view);
            }
        }
    };

    /**
     * KVLayer confirmed our Block-request
     */
    protected final Handler<BlockOk> blockOkHandler = new Handler<BlockOk>() {
        @Override
        public void handle(BlockOk blockOk) {
            LOG.debug("Received BlockOK from KV-layer");
            blocked = true;
        }
    };

    /**
     * Received flush-request from leader that wants to install a new view.
     */
    protected final ClassMatchedHandler<FlushReq, BEB_Deliver> flushReqHandler = new ClassMatchedHandler<FlushReq, BEB_Deliver>() {
        @Override
        public void handle(FlushReq flushReq, BEB_Deliver beb_deliver) {
            LOG.debug("Received Flush-request");
            if ((flushReq.oldView == viewId || viewId == 0) && flushReq.viewId == pendingViews.peek().id)
                trigger(new Message(selfPid.netAddress, pendingViews.peek().leader.netAddress, new Flush(latestUpdate, pendingViews.peek().id, viewId, selfPid)), net);
        }
    };

    /**
     * Received new view to be installed by leader
     */
    protected final ClassMatchedHandler<ViewInstall, BEB_Deliver> viewInstallHandler = new ClassMatchedHandler<ViewInstall, BEB_Deliver>() {
        @Override
        public void handle(ViewInstall viewInstall, BEB_Deliver beb_deliver) {
            LOG.debug("Installed new view");
            blocked = false;
            flushing = false;
            currentView = viewInstall.view;
            latestUpdate = viewInstall.latestUpdate;
            viewId = viewInstall.view.id;
            trigger(currentView, vSyncPort);
            if (pendingViews.size() > 0)
                pendingViews.remove();
            trigger(new VS_Deliver(latestUpdate, currentView.leader, viewId), vSyncPort);
        }
    };

    /**
     * Received Flush from member in the view
     */
    protected final ClassMatchedHandler<Flush, Message> flushHandler = new ClassMatchedHandler<Flush, Message>() {
        @Override
        public void handle(Flush flush, Message context) {
            LOG.debug("Received Flush");
            if ((flush.oldView == viewId || flush.oldView == 0) && flush.viewId == pendingViews.peek().id && pendingViews.peek().leader.equals(selfPid)) {
                flushes.add(flush.source);
                if (flush.latestUpdate != null && flush.latestUpdate.timestamp > latestUpdate.timestamp)
                    latestUpdate = flush.latestUpdate;
            }
        }
    };

    /**
     * Someone wants to join and we are leader, forward to GMS to make the decision.
     */
    protected final Handler<GMSJoin> gmsJoinHandler = new Handler<GMSJoin>() {
        @Override
        public void handle(GMSJoin gmsJoin) {
            trigger(gmsJoin, gmsPort);
        }
    };

    {
        subscribe(gmsJoinHandler, vSyncPort);
        subscribe(viewInstallHandler, broadcastPort);
        subscribe(flushReqHandler, broadcastPort);
        subscribe(deliverHandler, broadcastPort);
        subscribe(viewHandler, gmsPort);
        subscribe(accHandler, net);
        subscribe(blockOkHandler, vSyncPort);
        subscribe(broadcastHandler, vSyncPort);
        subscribe(vSyncInitHandler, vSyncPort);
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(flushHandler, net);
        subscribe(netBroadcastHandler, net);
    }

}
