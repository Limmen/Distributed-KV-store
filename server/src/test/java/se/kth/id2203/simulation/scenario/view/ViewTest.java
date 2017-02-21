package se.kth.id2203.simulation.scenario.view;


import java.util.*;

import org.junit.Assert;

import se.kth.id2203.gms.events.View;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

/**
 * 
 * Test if KV service gets right view from the VSync service. 
 * If this test passes then the BEB,EPFD, Omega and GMS works as well. 
 * 
 */
public class ViewTest {
	
    private static final int SERVERS = 5;
    private static final int REPLICATION_DEGREE = 3;
    private static final int CRASHES = 2;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String[] args) {
        
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario viewTestScenario = ScenarioGen.viewTest(SERVERS, REPLICATION_DEGREE, CRASHES);
       
        
        viewTestScenario.simulate(LauncherComp.class);
        
        List<ViewWrapper> views = new ArrayList<>();
        
        for(int i = 3; i <= SERVERS; i++){
        	String ip = "192.168.0." + i;
        	
        	HashMap<String,Object> view = res.get(ip, HashMap.class);
        	
        	String leader = (String) view.get("leader");
        	Set<String> members = (Set<String>) view.get("members");
        	long id = (Long) view.get("id");
        	
        	ViewWrapper viewWrapper = new ViewWrapper();
        	viewWrapper.id = id;
        	viewWrapper.leader = leader;
        	viewWrapper.members = members;
        	
        	
        	views.add(viewWrapper);
        	
        }
        
        
        ViewWrapper wp = views.get(0);
        
        Assert.assertEquals(3, wp.members.size());
        Assert.assertEquals(3, wp.id);
        
        
        for (ViewWrapper viewWrapper : views) {
			Assert.assertTrue(viewWrapper.equals(wp));
		}
        
    }
      
}
