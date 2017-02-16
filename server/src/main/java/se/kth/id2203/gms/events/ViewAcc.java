package se.kth.id2203.gms.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Event to indicate that node acknowledges a view-proposal
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class ViewAcc implements KompicsEvent, Serializable {

    public final long viewId;

    public ViewAcc(long viewId) {
        this.viewId = viewId;
    }
}
