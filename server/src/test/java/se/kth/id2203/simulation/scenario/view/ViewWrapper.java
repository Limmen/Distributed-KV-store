package se.kth.id2203.simulation.scenario.view;

import java.util.Set;

public class ViewWrapper{
	
	@Override
	public String toString() {
		return "ViewWrapper [leader=" + leader + ", members=" + members + ", id=" + id + "]";
	}
	public String leader;
	public Set<String> members;
	public long id;
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((leader == null) ? 0 : leader.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ViewWrapper other = (ViewWrapper) obj;
		if (id != other.id)
			return false;
		if (leader == null) {
			if (other.leader != null)
				return false;
		} else if (!leader.equals(other.leader))
			return false;
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		return true;
	}
	
	
}
