package se.kth.id2203.vsync.timeout;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class VSyncTimeout extends Timeout {

	public VSyncTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}

}
