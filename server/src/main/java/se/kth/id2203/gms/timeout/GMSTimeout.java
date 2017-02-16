package se.kth.id2203.gms.timeout;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class GMSTimeout extends Timeout {

	public GMSTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}

}
