package se.kth.id2203.epfd.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

/**
 * Suspect process
 */
public class Suspect implements KompicsEvent {
	public final PID suspected;

	public Suspect(PID suspected) {
		this.suspected = suspected;
	}
}
