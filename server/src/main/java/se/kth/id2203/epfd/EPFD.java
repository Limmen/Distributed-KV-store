package se.kth.id2203.epfd;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.epfd.events.*;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.epfd.timeout.EPFDTimeout;
import se.kth.id2203.networking.UDPMessage;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.PID;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Eventually Perfect Failure Detector, strong completeness and eventually strong accuracy.
 * Encapsulates timing assumption of partially synchronous systems
 *
 * @author Konstantin Sozinov
 */
public class EPFD extends ComponentDefinition {

    /* Ports */
    protected final Negative<EPFDPort> epfd = provides(EPFDPort.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    /* Fields */
    final static Logger LOG = LoggerFactory.getLogger(EPFD.class);
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private PID selfPid;
    private final long delta = config().getValue("id2203.project.epfd.delta", Long.class);
    private UUID timeoutId;
    private long seqnum = 0;
    private long delay;
    private Set<PID> all = new HashSet<>();
    private Set<PID> alive = new HashSet<>();
    private Set<PID> suspected = new HashSet<>();

    /**
     * Setup timer
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            delay = delta;
            selfPid = new PID(self, 0);
            LOG.info("Starting epfd with delta {} and delay {}", delta, delay);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(delay, delay);
            spt.setTimeoutEvent(new EPFDTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    /**
     * Initialize failure detection of set of nodes
     */
    protected final Handler<EPFDInit> initHandler = new Handler<EPFDInit>() {
        @Override
        public void handle(EPFDInit epfdInit) {
            LOG.debug("EPFD Initialized, monitoring {} processes", epfdInit.nodes.size());
            selfPid = epfdInit.pid;
            alive = new HashSet<>(ImmutableSet.copyOf(epfdInit.nodes));
            all = new HashSet<>(ImmutableSet.copyOf(epfdInit.nodes));
            suspected = new HashSet<>();
            seqnum = 0;
            delay = delta;
        }
    };

    /**
     * New nodes to monitor
     */
    protected final Handler<Reconfigure> reconfHandler = new Handler<Reconfigure>() {
        @Override
        public void handle(Reconfigure reconfigure) {
            all.addAll(reconfigure.nodes);
            alive.addAll(reconfigure.nodes);
            LOG.debug("EPFD Reconfigured, monitoring {} nodes", all.size());
        }
    };

    /**
     * Timeout, suspect processes that have'nt responded and restore processes that did
     */
    protected final Handler<EPFDTimeout> timeoutHandler = new Handler<EPFDTimeout>() {
        @Override
        public void handle(EPFDTimeout event) {
            if (!(Sets.intersection(alive, suspected).size() == 0)) {
                LOG.info("We detected premature-crash, increasing timeout with delta {}", delta);
                delay = delay + delta;
            }
            seqnum++;
            for (PID node : all) {
                if (!alive.contains(node) && !suspected.contains(node)) {
                    LOG.error("The node {} was suspected...", node.toString());
                    suspected.add(node);
                    trigger(new Suspect(node), epfd);
                } else if (alive.contains(node) && suspected.contains(node)) {
                    LOG.error("Got reply from a suspected {} node :) restore it", node.toString());
                    suspected.remove(node);
                    trigger(new Restore(node), epfd);
                }
                HBRequest req = new HBRequest(seqnum);
                trigger(new UDPMessage(selfPid.netAddress, node.netAddress, req), net);
            }
            alive = new HashSet<>();
        }
    };

    /**
     * Someone sent heartbeat-request, respond to it.
     */
    protected final ClassMatchedHandler<HBRequest, UDPMessage> requestHandler = new ClassMatchedHandler<HBRequest, UDPMessage>() {

        @Override
        public void handle(HBRequest event, UDPMessage msg) {
            HBReply reply = new HBReply(event.seqnum, event.id, selfPid);
            trigger(new UDPMessage(selfPid.netAddress, msg.getSource(), reply), net);
        }
    };

    /**
     * Someone responded to our heartbeat-request
     */
    protected final ClassMatchedHandler<HBReply, UDPMessage> replyHandler = new ClassMatchedHandler<HBReply, UDPMessage>() {

        @Override
        public void handle(HBReply event, UDPMessage msg) {
            if (event.seqnum == seqnum || suspected.contains(event.source)) {
                alive.add(event.source);
            }
        }
    };


    {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(requestHandler, net);
        subscribe(replyHandler, net);
        subscribe(initHandler, epfd);
        subscribe(reconfHandler, epfd);
    }

}
