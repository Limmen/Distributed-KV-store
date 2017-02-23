package se.kth.id2203.gms.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Set;

/**
 * GroupView
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class View implements KompicsEvent, Serializable {

	public final Set<PID> members;
    public final long id;
    public final PID leader;

    public View(Set<PID> members, long id, PID leader) {
        this.members = members;
        this.id = id;
        this.leader = leader;
    }

    public boolean sameView(Set<PID> nodes, PID leader2){
        if(leader == null)
            return false;
        if(leader2 == null)
            return false;
        if(!leader.equals(leader2))
            return false;
        if(nodes.size() != members.size())
            return false;
        for(PID pid : members){
            if(!nodes.contains(pid))
                return false;
        }
        return true;
    }

    @Override
	public String toString() {
		return "View [members=" + members + ", id=" + id + ", leader=" + leader + "]";
	}
}
