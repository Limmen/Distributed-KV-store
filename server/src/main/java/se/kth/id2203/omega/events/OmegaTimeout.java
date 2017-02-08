package se.kth.id2203.omega.events;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class OmegaTimeout extends Timeout {

	protected OmegaTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}

}
