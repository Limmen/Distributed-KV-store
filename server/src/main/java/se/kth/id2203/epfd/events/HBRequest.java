package se.kth.id2203.epfd.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Request heartbeat from some process
 */
public class HBRequest implements KompicsEvent,Serializable {

	private static final long serialVersionUID = -7977493649547332563L;
	
	public final UUID id;
	public final long seqnum;
	
	public HBRequest(long seqnum){
		id = UUID.randomUUID();
		this.seqnum = seqnum;
	}
	
	
}
