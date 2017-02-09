package se.kth.id2203.gms.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

public class GMSInit implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public GMSInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
