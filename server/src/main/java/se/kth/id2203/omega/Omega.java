package se.kth.id2203.omega;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.epfd.events.EPFDInit;
import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.omega.events.OmegaInit;
import se.kth.id2203.omega.events.OmegaTimeout;
import se.kth.id2203.omega.events.Trust;
import se.kth.id2203.omega.ports.OmegaPort;
import se.sics.kompics.*;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Omega extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(Omega.class);

    //private Component epfd = null;

    private NetAddress leader;
    private Set<NetAddress> all = new HashSet<>();
    private Set<NetAddress> suspected = new HashSet<>();

    protected final Negative<OmegaPort> omegaPort = provides(OmegaPort.class);
    protected final Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    private UUID timeoutId;

    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            long timeout = config().getValue("id2203.project.omega.timeout", Long.class);
            LOG.info("Starting OMEGA with delay {}", timeout);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new OmegaTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
            LOG.debug("Timeout set");
        }
    };

    protected final Handler<OmegaInit> omegaInitHandler = new Handler<OmegaInit>() {
        @Override
        public void handle(OmegaInit omegaInit) {
            LOG.debug("Omega Initialized");
            suspected = new HashSet<>();
            leader = null;
            all = ImmutableSet.copyOf(omegaInit.nodes);
            trigger(new EPFDInit(ImmutableSet.copyOf(omegaInit.nodes)), epfdPort);
        }
    };

    protected final Handler<OmegaTimeout> timeoutHandler = new Handler<OmegaTimeout>() {
        @Override
        public void handle(OmegaTimeout event) {
            NetAddress max = maxRank(Sets.difference(all, suspected));
            if (max != null && (leader == null || !leader.equals(max))) {
                leader = max;
                trigger(new Trust(leader), omegaPort);
                LOG.info("New Leader elected: {}", leader);
            }
        }
    };

    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {
        @Override
        public void handle(Suspect event) {
            suspected.add(event.suspected);
        }
    };

    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {
        @Override
        public void handle(Restore event) {
            suspected.remove(event.restored);
        }
    };

    private NetAddress maxRank(Set<NetAddress> nodes) {
        if (nodes.size() < 1)
            return null;
        else
            return Collections.max(nodes);
    }

    {
        subscribe(restoreHandler, epfdPort);
        subscribe(suspectHandler, epfdPort);
        subscribe(timeoutHandler, timer);
        subscribe(startHandler, control);
        subscribe(omegaInitHandler, omegaPort);
    }
}
