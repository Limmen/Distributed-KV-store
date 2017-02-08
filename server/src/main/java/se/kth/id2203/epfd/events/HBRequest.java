package se.kth.id2203.epfd.events;

import java.io.Serializable;
import java.util.UUID;

import se.sics.kompics.KompicsEvent;

public class HBRequest implements KompicsEvent,Serializable {

	private static final long serialVersionUID = -7977493649547332563L;
	
	public final UUID id;
	public final long seqnum;
	
	public HBRequest(long seqnum){
		id = UUID.randomUUID();
		this.seqnum = seqnum;
	}
	
	
}
