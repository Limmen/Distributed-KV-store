package se.kth.id2203.gms.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to GMS to initialize group membership service for a set of nodes
 */
public class GMSInit implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public GMSInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
