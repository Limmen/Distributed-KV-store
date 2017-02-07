package se.kth.id2203.epfd.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.KompicsException;

public class Suspect implements KompicsEvent {
	public final NetAddress suspected;

	public Suspect(NetAddress suspected) {
		this.suspected = suspected;
	}
}
