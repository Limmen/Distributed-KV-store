package se.kth.id2203.kvstore.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

public class ReplicationInit implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public ReplicationInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
