package se.kth.id2203.kvstore.events;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class KVServiceTimeout extends Timeout {

	public KVServiceTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}

}