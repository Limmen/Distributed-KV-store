package se.kth.id2203.omega.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class Trust implements KompicsEvent {
	
	public final NetAddress trusted;

	public Trust(NetAddress trusted) {
		this.trusted = trusted;
	}
}
