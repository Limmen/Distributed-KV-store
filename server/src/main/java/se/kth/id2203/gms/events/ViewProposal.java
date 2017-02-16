package se.kth.id2203.gms.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * ViewProposal sent by process that thinks it should be leader in a new view
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class ViewProposal implements KompicsEvent, Serializable {

    public final View view;

    public ViewProposal(View view) {
        this.view = view;
    }
}
