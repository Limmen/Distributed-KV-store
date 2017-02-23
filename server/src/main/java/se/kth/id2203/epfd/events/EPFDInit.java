package se.kth.id2203.epfd.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to EPFD containing set of nodes to monitor
 */
public class EPFDInit implements KompicsEvent {
	
	public final Set<PID> nodes;
	public final PID pid;
	
	public EPFDInit(Set<PID> nodes, PID pid){
		this.nodes = nodes;
		this.pid = pid;
	}

}
