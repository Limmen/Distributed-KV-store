package se.kth.id2203.vsync;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.gms.events.GMSInit;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.gms.ports.GMSPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
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
    final static Logger LOG = LoggerFactory.getLogger(VSyncService.class);
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private View currentView;
    private long viewId = 0;
    private boolean flushing;
    private boolean blocked;
    private StateUpdate latestUpdate;
    private Queue<View> pendingViews = new LinkedList<>();
    private Set<NetAddress> flushes = new HashSet<>();
    private UUID timeoutId;
    private Set<NetAddress> accs = new HashSet<>();
    private Queue<StateUpdate> pendingUpdates;
    private StateUpdate pendingUpdate;

    /**
     * Initialize timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            long timeout = 4000;
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
            trigger(new GMSInit(ImmutableSet.copyOf(vSyncInit.nodes)), gmsPort);
        }
    };

    /**
     * Timeout, if we are leader of new view and have pending view, attempt to install new view by collecting acks
     */
    protected final Handler<VSyncTimeout> timeoutHandler = new Handler<VSyncTimeout>() {
        @Override
        public void handle(VSyncTimeout event) {
            if (pendingViews.size() > 0 && !flushing) {
                flushing = true;
                LOG.debug("Pending view change.. triggering Block request to KV-layer");
                trigger(new Block(), vSyncPort);
            }
            if (pendingViews.size() > 0 && flushing && blocked && pendingViews.peek().leader.sameHostAs(self)) {
                Set<NetAddress> notFlushed = new HashSet<>();
                for (NetAddress member : pendingViews.peek().members) {
                    if (!flushes.contains(member))
                        notFlushed.add(member);
                }
                try {
                    if (notFlushed.size() > 0) {
                        LOG.debug("Missing flushes before new view can be installed, re-sending flushrequest..");
                        trigger(new BEB_Broadcast(new FlushReq(pendingViews.peek().id, viewId), notFlushed), broadcastPort);
                    } else {
                        LOG.debug("Received all flushes, installing new view");
                        currentView = pendingViews.remove();
                        viewId = currentView.id;
                        trigger(new BEB_Broadcast(new ViewInstall(currentView, latestUpdate), currentView.members), broadcastPort);
                    }
                } catch (Exception e) {
                    LOG.debug("Exception!");
                    e.printStackTrace();
                }
            }
            if(pendingUpdates.size() > 0 && pendingUpdate == null){
            	pendingUpdate = pendingUpdates.poll();
            	accs = new HashSet<>();
            }
            if (currentView != null && currentView.leader.sameHostAs(self) && pendingUpdate != null) {
                Set<NetAddress> notAcked = new HashSet<>();
                for (NetAddress member : currentView.members) {
                    if (!accs.contains(member))
                        notAcked.add(member);
                }
                if (notAcked.size() > 0) {
                	LOG.warn("Resending update to backups waiting for {} nodes ", notAcked.size());
                    trigger(new BEB_Broadcast(new VS_Deliver(pendingUpdate, self, viewId), notAcked), broadcastPort);
                } else {
                    trigger(new VS_Deliver(new WriteComplete(pendingUpdate.id), self, viewId), vSyncPort);
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
            if (currentView.leader.sameHostAs(self)) {
                LOG.debug("VSyncService leader received request, sending broadcast");
                VS_Deliver vs_deliver = new VS_Deliver(vs_broadcast.payload, self, viewId);
                pendingUpdates.add(vs_broadcast.payload);
                trigger(new BEB_Broadcast(vs_deliver, currentView.members), broadcastPort);
            } else {
                LOG.debug("VSyncService member received request, forwarding to leader");
                trigger(new Message(self, currentView.leader, vs_broadcast), net);
            }
        }
    }

    /**
     * Received operation from leader-broadcast, deliver to KVLayer.
     */
    protected final ClassMatchedHandler<VS_Deliver, BEB_Deliver> deliverHandler = new ClassMatchedHandler<VS_Deliver, BEB_Deliver>() {
        @Override
        public void handle(VS_Deliver vs_deliver, BEB_Deliver beb_deliver) {
            if (vs_deliver.viewId == viewId && vs_deliver.source.sameHostAs(currentView.leader)) {
                LOG.debug("Received StateUpdate from leader in view, delivering to application");
                latestUpdate = (StateUpdate) vs_deliver.payload;
                UpdateAcc acc = new UpdateAcc(latestUpdate.id);
                LOG.warn("Sending acc to {} ", beb_deliver.source);
                trigger(new Message(self,beb_deliver.source,acc),net);
                trigger(vs_deliver, vSyncPort);
            }
        }
    };
    
    protected final ClassMatchedHandler<UpdateAcc,Message > accHandler = new ClassMatchedHandler<UpdateAcc, Message>() {
		
		@Override
		public void handle(UpdateAcc content, Message context) {
			LOG.warn("Received acc from {} ", context.getSource());
			accs.add(context.getSource());
		}
	};

    /**
     * Received view from GMS, if we are leader we will try to install it, otherwise wait for other leader to try to
     * install it.
     */
    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            if (view.leader.sameHostAs(self)) {
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
            if (flushReq.oldView == viewId && flushReq.viewId == pendingViews.peek().id)
                trigger(new Message(self, pendingViews.peek().leader, new Flush(latestUpdate, pendingViews.peek().id, viewId)), net);
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
            if (flush.oldView == viewId && flush.viewId == pendingViews.peek().id && pendingViews.peek().leader.sameHostAs(self)) {
                flushes.add(context.getSource());
                if (flush.latestUpdate != null && flush.latestUpdate.timestamp > latestUpdate.timestamp)
                    latestUpdate = flush.latestUpdate;
            }
        }
    };

    {
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
