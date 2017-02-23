package se.kth.id2203.gms.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to GMS to initialize group membership service for a set of nodes
 */
public class GMSInit implements KompicsEvent {

	public final Set<PID> nodes;
	public final PID self;

	public GMSInit(Set<PID> nodes, PID self){
		this.nodes = nodes;
		this.self = self;
	}

}
