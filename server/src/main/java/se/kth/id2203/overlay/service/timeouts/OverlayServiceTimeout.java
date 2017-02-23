package se.kth.id2203.overlay.service.timeouts;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * @author Kim Hammar on 2017-02-22.
 */
public class OverlayServiceTimeout extends Timeout {

    public OverlayServiceTimeout(SchedulePeriodicTimeout request) {
        super(request);
    }
}
