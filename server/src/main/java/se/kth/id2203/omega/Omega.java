package se.kth.id2203.omega;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import se.kth.id2203.epfd.EPFD;
import se.kth.id2203.epfd.events.EPFDInit;
import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.epfd.timeout.EPFDTimeout;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.omega.events.OmegaInit;
import se.kth.id2203.omega.events.OmegaTimeout;
import se.kth.id2203.omega.events.Trust;
import se.kth.id2203.omega.ports.OmegaPort;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

public class Omega extends ComponentDefinition  {

	final static Logger LOG = LoggerFactory.getLogger(Omega.class);
	
	private final Component epfd;
	
	private NetAddress leader;
	private final Set<NetAddress> all;
	private final Set<NetAddress> suspected;
	
	protected final Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
	protected final Negative<OmegaPort> omegaPort = provides(OmegaPort.class);
	protected final Positive<Timer> timer = requires(Timer.class);
	
	private UUID timeoutId;
	
	public Omega(OmegaInit init){
		epfd = create(EPFD.class, new EPFDInit(init.nodes));
		suspected = new HashSet<>();
		leader = null;
		all = ImmutableSet.copyOf(init.nodes);
	}
	
	protected final Handler<Start> startHandler = new Handler<Start>() {
		@Override
		public void handle(Start e) {
			long timeout = config().getValue("id2203.project.omega.timeout", Long.class);
			LOG.info("Starting OMEGA with delay {}",timeout);
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
			spt.setTimeoutEvent(new EPFDTimeout(spt));
			trigger(spt, timer);
			timeoutId = spt.getTimeoutEvent().getTimeoutId();
		}
	};
	
	Handler<OmegaTimeout> timeoutHanlder = new Handler<OmegaTimeout>(){
		@Override
		public void handle(OmegaTimeout event) {
			leader = maxRank(Sets.difference(all, suspected));
			trigger(new Trust(leader),omegaPort);
		}	
	};
	
	Handler<Suspect> suspectHandler = new Handler<Suspect>(){
		@Override
		public void handle(Suspect event) {
			suspected.add(event.suspected);
		}
	};

	Handler<Restore> restoreHandler = new Handler<Restore>(){
		@Override
		public void handle(Restore event) {
			suspected.remove(event.restored);
		}
	};
	
	private NetAddress maxRank(Set<NetAddress> nodes){
		return Collections.max(nodes);
	}
	
	{
		subscribe(restoreHandler, epfdPort);
		subscribe(suspectHandler, epfdPort);
		subscribe(timeoutHanlder, timer);
		subscribe(startHandler, control);
	}
}
