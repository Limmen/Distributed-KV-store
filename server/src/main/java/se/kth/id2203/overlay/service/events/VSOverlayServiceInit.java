package se.kth.id2203.overlay.service.events;

import se.kth.id2203.overlay.PID;
import se.kth.id2203.overlay.lookuptable.LookupTable;
import se.sics.kompics.KompicsEvent;

/**
 * @author Kim Hammar on 2017-02-22.
 */
public class VSOverlayServiceInit implements KompicsEvent {

    public final LookupTable lookupTable;
    public final PID pid;

    public VSOverlayServiceInit(LookupTable lookupTable, PID pid) {
        this.lookupTable = lookupTable;
        this.pid = pid;
    }
}
