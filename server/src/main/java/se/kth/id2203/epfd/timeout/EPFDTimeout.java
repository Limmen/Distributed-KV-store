package se.kth.id2203.epfd.timeout;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class EPFDTimeout extends Timeout {

	public EPFDTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}
}
