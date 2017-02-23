package se.kth.id2203.gms;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.epfd.events.Reconfigure;
import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.gms.events.*;
import se.kth.id2203.gms.ports.GMSPort;
import se.kth.id2203.gms.timeout.GMSTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.omega.events.OmegaInit;
import se.kth.id2203.omega.events.Trust;
import se.kth.id2203.omega.ports.OmegaPort;
import se.kth.id2203.overlay.PID;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Group membership service, provides monotonicity, agreement and completeness but not accuracy.
 * Resilience: N/2 -1, can only tolerate a minority of processes failing, view always contains at least a
 * quorum of processes.
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class GMS extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Negative<GMSPort> gmsPort = provides(GMSPort.class);
    protected final Positive<OmegaPort> omegaPort = requires(OmegaPort.class);
    protected final Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    protected final Positive<BEBPort> broadcastPort = requires(BEBPort.class);
    /* Fields */
    final static Logger LOG = LoggerFactory.getLogger(GMS.class);
    private long viewId;
    private Set<PID> members;
    private PID selfPid;
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final int replicationDegree = config().getValue("id2203.project.replicationDegree", Integer.class);
    private PID leader = null;
    private Role role;
    private UUID timeoutId;
    private View currentView;
    private View pendingView = null;
    private Set<PID> acks = new HashSet();

    /**
     * Setup timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            selfPid = new PID(self, 0);
            long timeout = config().getValue("id2203.project.gms.timeout", Long.class);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new GMSTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    /**
     * Initialize group membership service for a set of processes
     */
    protected final Handler<GMSInit> gmsInitHandler = new Handler<GMSInit>() {
        @Override
        public void handle(GMSInit gmsInit) {
            LOG.debug("GMS Initialized");
            selfPid = gmsInit.self;
            viewId = 0;
            members = new HashSet<>(gmsInit.nodes);
            role = Role.WORKER;
            currentView = new View(ImmutableSet.copyOf(members), viewId, null);
            trigger(new OmegaInit(ImmutableSet.copyOf(gmsInit.nodes), selfPid), omegaPort);
        }
    };

    /**
     * If new view should be installed and we are the leader, try to collect quorum of ACKS and then commit the new
     * view.
     */
    protected final Handler<GMSTimeout> timeoutHandler = new Handler<GMSTimeout>() {
        @Override
        public void handle(GMSTimeout event) {
            if (role == Role.LEADER && members != null && members.size() < replicationDegree){
                LOG.warn("Under-replicated or partitioned, crash to make place for a primary partition if such exists.");
                Kompics.shutdown();
            }
            if (members != null && members.size() != currentView.members.size() && pendingView == null)
                viewChange();
            if (role == Role.LEADER && pendingView != null && pendingView.id > currentView.id) {
                Set<PID> notAcked = new HashSet<>();
                for (PID member : pendingView.members) {
                    if (!acks.contains(member))
                        notAcked.add(member);
                }
                if (pendingView.members.size() - notAcked.size() < replicationDegree) { //quorum not yet acked
                    LOG.debug("I'm leader in my currentView, sending currentView proposal and collecting ACKs");
                    trigger(new BEB_Broadcast(new ViewProposal(pendingView), notAcked, selfPid), broadcastPort);
                } else {
                    trigger(new BEB_Broadcast(new ViewCommit(pendingView), pendingView.members, selfPid), broadcastPort);
                    currentView = pendingView;
                    pendingView = null;
                    acks = new HashSet<>();
                }
            }
        }
    };

    /**
     * Omega indicates that a new leader is elected
     */
    protected final Handler<Trust> trustedHandler = new Handler<Trust>() {
        @Override
        public void handle(Trust trusted) {
            LOG.info("GMS: New leader elected: {}", trusted.trusted);
            leader = trusted.trusted;
            if (leader.equals(selfPid)) {
                role = Role.LEADER;
            } else
                role = Role.WORKER;
            viewChange();
        }
    };

    /**
     * EPFD suspect some process
     */
    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {
        @Override
        public void handle(Suspect event) {
            if (role == Role.LEADER) {
                LOG.info("GMS: Leader detected crash, updating currentView");
            }
            members.remove(event.suspected);
            viewChange();
        }
    };

    /**
     * Have received information from EPFD/Omega that should trigger a view change, if this node views itself as leader
     * it will attempt to propose the new view to a quorum.
     */
    private void viewChange() {
        if (!currentView.sameView(members, leader) && role == Role.LEADER) {
            acks = new HashSet<>();
            viewId++;
            pendingView = new View(ImmutableSet.copyOf(members), viewId, selfPid);
        }
    }

    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {
        @Override
        public void handle(Restore event) {
            //Possible optimization to add previosuly suspected and removed nodes, for simplicity we don't.
        }
    };

    /**
     * Received new view-proposal from some process.
     * Only ACK a view-proposal from the leader that Omega indicated. Otherwise ignore.
     */
    protected final ClassMatchedHandler<ViewProposal, BEB_Deliver> viewProposalHandler = new ClassMatchedHandler<ViewProposal, BEB_Deliver>() {
        @Override
        public void handle(ViewProposal viewProposal, BEB_Deliver beb_deliver) {
            LOG.debug("GMS Peer received currentView proposal");
            if (leader != null && leader.equals(viewProposal.view.leader))
                trigger(new Message(selfPid.netAddress, viewProposal.view.leader.netAddress, new ViewAcc(viewProposal.view.id, selfPid)), net);
        }
    };

    /**
     * Received a view-commit, i.e some leader received quorum of ACKS and committed a new view.
     */
    protected final ClassMatchedHandler<ViewCommit, BEB_Deliver> viewCommitHandler = new ClassMatchedHandler<ViewCommit, BEB_Deliver>() {
        @Override
        public void handle(ViewCommit viewCommit, BEB_Deliver beb_deliver) {
            LOG.debug("GMS Peer received currentView commit, delivering new currentView");
            currentView = viewCommit.view;
            pendingView = null;
            acks = new HashSet<>();
            viewId = currentView.id;
            members = new HashSet<>(ImmutableSet.copyOf(currentView.members));
            trigger(new OmegaInit(currentView.members, selfPid), omegaPort);
            trigger(currentView, gmsPort);
        }
    };

    /**
     * Received ACK from some member in group
     */
    protected final ClassMatchedHandler<ViewAcc, Message> ackHandler = new ClassMatchedHandler<ViewAcc, Message>() {
        @Override
        public void handle(ViewAcc viewAcc, Message message) {
            LOG.debug("Received ACK for currentView proposal");
            if (viewAcc.viewId == pendingView.id)
                acks.add(viewAcc.source);
        }
    };

    /**
     * Join request, only allow node to join if this replication-group is not full.
     */
    protected final Handler<GMSJoin> joinHandler = new Handler<GMSJoin>() {
        @Override
        public void handle(GMSJoin gmsJoin) {
            if (members.size() < replicationDegree * 2 - 1 && !members.contains(gmsJoin.node)) {
                members.add(gmsJoin.node);
                Set<PID> reconf = new HashSet<PID>();
                reconf.add(gmsJoin.node);
                trigger(new Reconfigure(reconf), omegaPort);
            }
        }
    };

    {
        subscribe(joinHandler, gmsPort);
        subscribe(ackHandler, net);
        subscribe(viewProposalHandler, broadcastPort);
        subscribe(viewCommitHandler, broadcastPort);
        subscribe(trustedHandler, omegaPort);
        subscribe(suspectHandler, epfdPort);
        subscribe(restoreHandler, epfdPort);
        subscribe(gmsInitHandler, gmsPort);
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
    }

    public enum Role {
        LEADER, WORKER
    }

}
