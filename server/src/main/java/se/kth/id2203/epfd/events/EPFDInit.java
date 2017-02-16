package se.kth.id2203.epfd.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Message sent to EPFD containing set of nodes to monitor
 */
public class EPFDInit implements KompicsEvent {
	
	public final Set<NetAddress> nodes;
	
	public EPFDInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
