package se.kth.id2203.epfd.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

/**
 * Restore suspicion of process
 */
public class Restore implements KompicsEvent {

	public final PID restored;

	public Restore(PID restored) {
		this.restored = restored;
	}

}
