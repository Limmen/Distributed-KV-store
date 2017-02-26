package se.kth.id2203.kvstore.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.HashMap;
import java.util.Set;

/**
 * Event sent to initialize KVService in a replicationgroup, sent by overlay after bootstrapping is completed
 */
public class ReplicationInit implements KompicsEvent {

	public final Set<PID> nodes;
	public final PID self;
	public final HashMap<Integer, String> keyValues;

	public ReplicationInit(Set<PID> nodes, PID self, HashMap<Integer, String> keyValues){
		this.nodes = nodes;
		this.self = self;
        this.keyValues = keyValues;
	}

}
