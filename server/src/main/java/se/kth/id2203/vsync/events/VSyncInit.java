package se.kth.id2203.vsync.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * Event to initialize view-synchronous service for a set of nodes.
 */
public class VSyncInit implements KompicsEvent {

	public final Set<PID> nodes;
    public final PID self;
    public final StateTransfer stateTransfer;

	public VSyncInit(Set<PID> nodes, PID self, StateTransfer stateTransfer){
		this.nodes = nodes;
        this.self = self;
        this.stateTransfer = stateTransfer;
    }

}
