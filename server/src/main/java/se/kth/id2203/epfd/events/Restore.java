package se.kth.id2203.epfd.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class Restore implements KompicsEvent {

	public final NetAddress restored;

	public Restore(NetAddress restored) {
		this.restored = restored;
	}

}
