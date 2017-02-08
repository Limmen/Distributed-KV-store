package se.kth.id2203.epfd;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.epfd.events.EPFDInit;
import se.kth.id2203.epfd.events.HBReply;
import se.kth.id2203.epfd.events.HBRequest;
import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.epfd.timeout.EPFDTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.ClassMatchedHandler;
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
	private long seqnum;
	private long delay;
	
	private final Set<NetAddress> all;
	private final Set<NetAddress> alive;
	private final Set<NetAddress> suspected;

	public EPFD(EPFDInit init) {
		alive = ImmutableSet.copyOf(init.nodes);
		all = ImmutableSet.copyOf(init.nodes);
		suspected = new HashSet<>();
		seqnum = 0;
		delta = config().getValue("id2203.project.epfd.delta", Long.class);
		delay = delta;
	}

	protected final Handler<Start> startHandler = new Handler<Start>() {
		@Override
		public void handle(Start e) {
			LOG.info("Starting epfd with delta {} and delay {}", delta, delay);
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(delay, delay);
			spt.setTimeoutEvent(new EPFDTimeout(spt));
			trigger(spt, timer);
			timeoutId = spt.getTimeoutEvent().getTimeoutId();
		}
	};

	Handler<EPFDTimeout> timeoutHandler = new Handler<EPFDTimeout>() {
		@Override
		public void handle(EPFDTimeout event) {
			if(Sets.intersection(alive, suspected).size() == 0){
				LOG.info("We detected premature-crash, increasing timeout with delta {}",delta);
				delay = delay + delta;
			}
			seqnum++;
			for (NetAddress node : all) {
				if(!alive.contains(node) && !suspected.contains(node)) {
					LOG.error("The node {} was suspected...",node.toString());
					suspected.add(node);
					trigger(new Suspect(node),epfd);
				} else if(alive.contains(node) && suspected.contains(node)){
					LOG.error("Got reply from a suspected {} node :) restore it",node.toString());
					suspected.remove(node);
					trigger(new Restore(node), epfd);
				}
				HBRequest req = new HBRequest(seqnum);
				trigger(new Message(self,node,req) ,net);
			}
			alive.clear();
		}
	};

	protected final ClassMatchedHandler<HBRequest, Message> requestHandler = new ClassMatchedHandler<HBRequest, Message>() {

		@Override
		public void handle(HBRequest event, Message msg) {
			HBReply reply = new HBReply(event.seqnum, event.id);
			trigger(new Message(self,msg.getSource(),reply),net);
		}
	};

	protected final ClassMatchedHandler<HBReply, Message> replyHandler = new ClassMatchedHandler<HBReply, Message>() {

		@Override
		public void handle(HBReply event, Message msg) {
			if(event.seqnum == seqnum || suspected.contains(msg.getSource())){
				alive.add(msg.getSource());
			}
		}
	};

	{
		subscribe(startHandler, control);
		subscribe(timeoutHandler, timer);
		subscribe(requestHandler, net);
		subscribe(replyHandler, net);
	}

}
