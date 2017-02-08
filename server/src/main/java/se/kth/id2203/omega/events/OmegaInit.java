package se.kth.id2203.omega.events;

import java.util.Set;

import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.omega.Omega;
import se.sics.kompics.Init;

public class OmegaInit extends Init<Omega> {
	
	public final Set<NetAddress> nodes;
	
	public OmegaInit(Set<NetAddress> nodes){
		this.nodes = nodes;
	}

}
