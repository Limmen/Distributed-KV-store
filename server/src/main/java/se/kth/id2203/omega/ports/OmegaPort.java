package se.kth.id2203.omega.ports;

import se.kth.id2203.omega.events.Trust;
import se.sics.kompics.PortType;

public class OmegaPort extends PortType {

	{
		indication(Trust.class);
	}
}
