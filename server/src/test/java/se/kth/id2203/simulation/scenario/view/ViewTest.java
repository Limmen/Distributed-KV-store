package se.kth.id2203.simulation.scenario.view;


import org.junit.Assert;
import org.junit.Test;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * 
 * Test if KV service gets right views from the VSync service.
 * If this test passes then the BEB,EPFD, Omega and GMS works as well. 
 * 
 */
public class ViewTest {
	
    private static final int SERVERS = 5;
    private static final int REPLICATION_DEGREE = 3;
    private static final int CRASHES = 2;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void ViewTest() {
        
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario viewTestScenario = ScenarioGen.viewTest(SERVERS, REPLICATION_DEGREE, CRASHES);

        /**
         * Boot up SERVERS in a cluster and a single replication group, wait for a while, then crash CRASHES of the servers
         */
        viewTestScenario.simulate(LauncherComp.class);

        /**
         * 3 servers should have survived the crash and installed a new view which excludes the crashed processes
         */
        for(int i = CRASHES+1; i <= SERVERS; i++){
        	String ip = "192.168.0." + i;
        	
        	ArrayList<HashMap<String,Object>> views = res.get(ip+"-views", ArrayList.class);
        	
        	Assert.assertEquals(2, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));
        	
        	ViewWrapper wp1 = fromHashMapToView(views.get(1));
        	
        	Assert.assertEquals(1, wp0.id);
        	Assert.assertEquals(5, wp0.members.size());        	
        	
        	Assert.assertEquals(2, wp1.id);
        	Assert.assertEquals(3, wp1.members.size());
        	
        }
        /**
         * 2 servers should have crashed and not got the second view.
         */
        for(int i = 1; i <= CRASHES; i++){
        	String ip = "192.168.0." + i;
        	
        	ArrayList<HashMap<String,Object>> views = res.get(ip+"-views", ArrayList.class);
        	
        	Assert.assertEquals(1, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));
        	
        	Assert.assertEquals(1, wp0.id);
        	Assert.assertEquals(5, wp0.members.size());
        }
        
        
        
    }
    
    private static ViewWrapper fromHashMapToView(HashMap<String,Object> from){
    	String leader = (String) from.get("leader");
    	Set<String> members = (Set<String>) from.get("members");
    	long id = (Long) from.get("id");
    	
    	ViewWrapper viewWrapper = new ViewWrapper();
    	viewWrapper.id = id;
    	viewWrapper.leader = leader;
    	viewWrapper.members = members;
    	return viewWrapper;
    }
      
}
