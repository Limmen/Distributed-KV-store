package se.kth.id2203.simulation.gv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;


/**
 * 
 * The GlobalView allows you to do three things:
 *		check which nodes are dead or alive
 *		set/get <key,values> shared globally
 *		tell the simulator to terminate this simulation
 * 
 * @author konstantin
 *
 */
public class SimulationObserver extends ComponentDefinition {
	 
	private static final Logger LOG = LoggerFactory.getLogger(SimulationObserver.class);
	    
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	// Fields we want to observe here
	// ...
	private final int deadNodes;
	
	private UUID timerId;
	
	public SimulationObserver(Init init){
		deadNodes = init.deadNodes;
		
		subscribe(handleStart, control);
        subscribe(handleCheck, timer);
	}
	
	/**
	 * 
	 * Schedule periodic check of global fields
	 */
	Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            schedulePeriodicCheck();
        }
    };
    
    @Override
    public void tearDown() {
        trigger(new CancelPeriodicTimeout(timerId), timer);
    }
    
    /*
     * 
     * Upon timeout, check our global fields
     * 
     */
    Handler<CheckTimeout> handleCheck = new Handler<CheckTimeout>() {
        @Override
        public void handle(CheckTimeout event) {
            GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
            
            if(gv.getDeadNodes().size() >= deadNodes) {
            	LOG.info("Terminating simulation as the {} are dead ", deadNodes);
            	gv.terminate();
            }
        }
    };
    
    private void schedulePeriodicCheck() {
        long period = config().getValue("kvstore.simulation.checktimeout", Long.class);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        CheckTimeout timeout = new CheckTimeout(spt);
        spt.setTimeoutEvent(timeout);
        trigger(spt, timer);
        timerId = timeout.getTimeoutId();
    }
    
    public static class CheckTimeout extends Timeout {

        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
	
	public static class Init extends se.sics.kompics.Init<SimulationObserver> {

		// Fields we want to observe here
		// ...
		private final int deadNodes;
		
        public Init(int deadNodes) {
            this.deadNodes = deadNodes;
        }
    }
}
