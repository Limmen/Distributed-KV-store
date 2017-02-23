package se.kth.id2203.overlay;

import se.kth.id2203.networking.NetAddress;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Kim Hammar on 2017-02-22.
 */
public class PID implements Serializable, Comparable<PID> {

    public final NetAddress netAddress;
    public final long pid;

    public PID(NetAddress netAddress, long pid) {
        this.netAddress = netAddress;
        this.pid = pid;
    }

    @Override
    public int compareTo(PID o) {
        if (this.pid > o.pid)
            return 1;
        if(this.pid < o.pid)
            return -1;
        return 0;
    }


    public static PID getPID(NetAddress address, Set<PID> all){
        for(PID node : all){
            if(address.equals(node.netAddress))
                return node;
        }
        return null;
    }

    @Override
    public String toString() {
        return "PID{" +
                "netAddress=" + netAddress +
                ", pid=" + pid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PID)) return false;

        PID pid1 = (PID) o;

        if (pid != pid1.pid) return false;
        return netAddress.equals(pid1.netAddress);

    }

    @Override
    public int hashCode() {
        int result = netAddress.hashCode();
        result = 31 * result + (int) (pid ^ (pid >>> 32));
        return result;
    }
}
