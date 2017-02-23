package se.kth.id2203.omega.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to Omega containing set of nodes to monitor for leader election
 */
public class OmegaInit implements KompicsEvent {
	
	public final Set<PID> nodes;
	public final PID self;
	
	public OmegaInit(Set<PID> nodes, PID self){
		this.nodes = nodes;
		this.self = self;
	}

}
