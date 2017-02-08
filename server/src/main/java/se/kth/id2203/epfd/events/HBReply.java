package se.kth.id2203.epfd.events;

import java.io.Serializable;
import java.util.UUID;

import se.sics.kompics.KompicsEvent;

public class HBReply implements KompicsEvent, Serializable {

	private static final long serialVersionUID = 3559489170214118550L;
	
	public final UUID id;
	public final long seqnum;
	
	public HBReply(long seqnum, UUID id){
		this.seqnum = seqnum;
		this.id = id;
	}

}
