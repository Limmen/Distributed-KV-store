package se.kth.id2203.gms.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class View implements KompicsEvent {

    public final Set<NetAddress> members;
    public final long id;
    public final NetAddress leader;

    public View(Set<NetAddress> members, long id, NetAddress leader) {
        this.members = members;
        this.id = id;
        this.leader = leader;
    }
}
