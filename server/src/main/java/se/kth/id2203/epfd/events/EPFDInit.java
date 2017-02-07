package se.kth.id2203.epfd.events;

import java.util.Set;

import se.kth.id2203.epfd.EPFD;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.Init;

public class EPFDInit extends Init<EPFD> {
	
	public final Set<NetAddress> nodes;
	
	public EPFDInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
