package se.kth.id2203.epfd.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

public class EPFDReconf implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public EPFDReconf(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
