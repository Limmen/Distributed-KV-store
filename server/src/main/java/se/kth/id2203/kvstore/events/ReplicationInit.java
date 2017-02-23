package se.kth.id2203.kvstore.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Event sent to initialize KVService in a replicationgroup, sent by overlay after bootstrapping is completed
 */
public class ReplicationInit implements KompicsEvent {

	public final Set<PID> nodes;
	public final PID self;

	public ReplicationInit(Set<PID> nodes, PID self){
		this.nodes = nodes;
		this.self = self;
	}

}
