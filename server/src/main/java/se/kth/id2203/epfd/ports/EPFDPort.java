package se.kth.id2203.epfd.ports;

import se.kth.id2203.epfd.events.Restore;
import se.kth.id2203.epfd.events.Suspect;
import se.sics.kompics.PortType;

public class EPFDPort extends PortType {
	
	{
		indication(Restore.class);
		indication(Suspect.class);
	}
}
