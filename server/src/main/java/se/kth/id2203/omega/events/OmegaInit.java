package se.kth.id2203.omega.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to Omega containing set of nodes to monitor for leader election
 */
public class OmegaInit implements KompicsEvent {
	
	public final Set<NetAddress> nodes;
	
	public OmegaInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
