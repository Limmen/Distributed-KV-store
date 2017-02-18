package se.kth.id2203.kvstore.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Event sent to initialize KVService in a replicationgroup, sent by overlay after bootstrapping is completed
 */
public class ReplicationInit implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public ReplicationInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
