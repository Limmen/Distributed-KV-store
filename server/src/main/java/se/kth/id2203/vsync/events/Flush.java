package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Flush-message that is used to ensure consistency before a new view is installed
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class Flush implements KompicsEvent, Serializable {

    public final StateUpdate latestUpdate;
    public final long viewId;
    public final long oldView;

    public Flush(StateUpdate latestUpdate, long viewId, long oldView) {
        this.latestUpdate = latestUpdate;
        this.viewId = viewId;
        this.oldView = oldView;
    }
}
