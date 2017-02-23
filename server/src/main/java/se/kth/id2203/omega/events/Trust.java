package se.kth.id2203.omega.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

/**
 * Message sent by Omega when new leader is elected
 */
public class Trust implements KompicsEvent {
	
	public final PID trusted;

	public Trust(PID trusted) {
		this.trusted = trusted;
	}
}
