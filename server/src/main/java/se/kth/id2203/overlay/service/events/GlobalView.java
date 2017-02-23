package se.kth.id2203.overlay.service.events;

import se.kth.id2203.overlay.lookuptable.LookupTable;
import se.sics.kompics.KompicsEvent;


/**
 * @author Kim Hammar on 2017-02-22.
 */
public class GlobalView implements KompicsEvent {

    public final LookupTable lookupTable;

    public GlobalView(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }
}
