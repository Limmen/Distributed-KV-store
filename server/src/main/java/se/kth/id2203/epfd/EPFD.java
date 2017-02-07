package se.kth.id2203.epfd;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.epfd.events.EPFDInit;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.epfd.timeout.EPFDTimeout;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsException;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;


public class EPFD extends ComponentDefinition {

	final static Logger LOG = LoggerFactory.getLogger(EPFD.class);
	
	protected final Negative<EPFDPort> epfd = provides(EPFDPort.class);
	protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final long delta;
    private UUID timeoutId;
    
    private final Set<NetAddress> alive;
    private final Set<NetAddress> suspected;
    
    public EPFD(EPFDInit init){
    	alive = ImmutableSet.copyOf(init.nodes);
    	suspected = new HashSet<>();
    	delta = config().getValue("id2203.project.epfd.delay", Long.class);
    }
    
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            LOG.info("Starting epfd with delta {}", delta);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(delta, delta);
            spt.setTimeoutEvent(new EPFDTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };
	
    Handler<EPFDTimeout> timeoutHandler = new Handler<EPFDTimeout>() {
        @Override
        public void handle(EPFDTimeout event) {
            //TODO: upon timeout do main loop
        	//trigger(new Ping(self, ponger), net);
        }
    };
    
    {
    	subscribe(startHandler, control);
    	subscribe(timeoutHandler, timer);
    }
    
    
}
