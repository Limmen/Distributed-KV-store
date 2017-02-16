package se.kth.id2203.epfd.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message sent in reply to hearbeat-request
 */
public class HBReply implements KompicsEvent, Serializable {

	private static final long serialVersionUID = 3559489170214118550L;
	
	public final UUID id;
	public final long seqnum;
	
	public HBReply(long seqnum, UUID id){
		this.seqnum = seqnum;
		this.id = id;
	}

}
