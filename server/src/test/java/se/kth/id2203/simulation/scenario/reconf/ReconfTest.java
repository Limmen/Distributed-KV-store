package se.kth.id2203.simulation.scenario.reconf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.junit.Assert;

import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.ScenarioGen;
import se.kth.id2203.simulation.scenario.view.ViewWrapper;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;


/*
 * 
 * This test will check if nodes get a correct view once new nodes joins one partition.
 * 
 * TODO: Test linearisability?
 * 
 * 
 */
public class ReconfTest {

	private static final int SERVERS = 3;
    private static final int REPLICATION_DEGREE = 3;
    private static final int JOINS = 2;
    
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();
    
    
    public static void main(String[] args){
    	long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario viewTestScenario = ScenarioGen.reconfTest(SERVERS, REPLICATION_DEGREE, JOINS);
       
        
        viewTestScenario.simulate(LauncherComp.class);
        
        for(int i = 1; i <= SERVERS; i++){
        	String ip = "192.168.0." + i;
        	
        	ArrayList<HashMap<String,Object>> views = res.get(ip, ArrayList.class);
        	
        	Assert.assertEquals(2, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));
        	
        	ViewWrapper wp1 = fromHashMapToView(views.get(1));
        	
        	Assert.assertEquals(1, wp0.id);
        	Assert.assertEquals(3, wp0.members.size());        	
        	
        	Assert.assertEquals(2, wp1.id);
        	Assert.assertEquals(5, wp1.members.size());
        	
        }
        
        for(int i = SERVERS+1; i <= SERVERS+JOINS; i++){
        	String ip = "192.168.0." + i;
        	
        	ArrayList<HashMap<String,Object>> views = res.get(ip, ArrayList.class);
        	
        	Assert.assertEquals(1, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));
        	
        	Assert.assertEquals(2, wp0.id);
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
