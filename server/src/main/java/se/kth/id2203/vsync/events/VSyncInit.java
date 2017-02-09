package se.kth.id2203.vsync.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

public class VSyncInit implements KompicsEvent {

	public final Set<NetAddress> nodes;

	public VSyncInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
