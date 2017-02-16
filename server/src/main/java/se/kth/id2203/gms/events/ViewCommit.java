package se.kth.id2203.gms.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * View-commit sent by leader to members after having collected quorum of ACKs
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class ViewCommit implements KompicsEvent, Serializable {

    public final View view;

    public ViewCommit(View view) {
        this.view = view;
    }
}
