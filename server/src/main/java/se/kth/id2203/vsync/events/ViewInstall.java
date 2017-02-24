package se.kth.id2203.vsync.events;

import se.kth.id2203.gms.events.View;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Event sent by leader to members of new view after it has collected all flushes.
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class ViewInstall implements KompicsEvent, Serializable {

    public final View view;
    public final StateTransfer latestUpdate;

    public ViewInstall(View view, StateTransfer latestUpdate) {
        this.view = view;
        this.latestUpdate = latestUpdate;
    }
}
