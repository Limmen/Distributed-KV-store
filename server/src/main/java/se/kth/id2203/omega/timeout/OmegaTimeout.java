package se.kth.id2203.omega.timeout;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class OmegaTimeout extends Timeout {

	public OmegaTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}

}
