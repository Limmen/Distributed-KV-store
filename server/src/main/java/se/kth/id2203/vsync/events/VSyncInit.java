package se.kth.id2203.vsync.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Event to initialize view-synchronous service for a set of nodes.
 */
public class VSyncInit implements KompicsEvent {

	public final Set<NetAddress> nodes;
    public final StateUpdate stateUpdate;

	public VSyncInit(Set<NetAddress> nodes, StateUpdate stateUpdate){
		this.nodes = nodes;
        this.stateUpdate = stateUpdate;
    }

}
