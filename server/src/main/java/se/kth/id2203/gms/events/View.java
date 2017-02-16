package se.kth.id2203.gms.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Set;

/**
 * GroupView
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class View implements KompicsEvent, Serializable {

    public final Set<NetAddress> members;
    public final long id;
    public final NetAddress leader;

    public View(Set<NetAddress> members, long id, NetAddress leader) {
        this.members = members;
        this.id = id;
        this.leader = leader;
    }

    public boolean sameView(Set<NetAddress> nodes, NetAddress leader2){
        if(leader == null)
            return false;
        if(leader2 == null)
            return false;
        if(!leader.sameHostAs(leader2))
            return false;
        if(leader.compareTo(leader2) != 0)
            return false;
        if(nodes.size() != members.size())
            return false;
        for(NetAddress netAddress : members){
            if(!nodes.contains(netAddress))
                return false;
        }
        return true;
    }

}
