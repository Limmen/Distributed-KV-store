package se.kth.id2203.simulation.scenario.reconf;

import org.junit.Assert;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.kth.id2203.simulation.scenario.view.ViewWrapper;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


/*
 * 
 * This test will check if nodes get a correct view once new nodes joins one partition.
 * Basically similar test as ViewTest but instead of testing that the view is updated once crashes occur this
 * file tests that views are updated when new nodes join a partition.
 * 
 * TODO: Test linearisability?
 *
 */
public class AddNodesToPartitionTest {

	private static final int SERVERS = 3;
    private static final int REPLICATION_DEGREE = 3;
    private static final int JOINS = 2;

    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();


    public static void main(String[] args){
    	long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario reconfigureTest = ScenarioGen.reconfTest(SERVERS, REPLICATION_DEGREE, JOINS, 50);
        res.put("trace", new ConcurrentLinkedQueue<>());

        /**
         * Boot up SERVERS in a cluster and a single replication group, wait for a while, then boot up JOINS which will
         * send requests to join one of the servers in the cluster. Since partitions = 3 and rep_degree (quorum) = 3,
         * there is space for 2 more in the partition so the new servers will be able to join the existing partition
         * not having to create a new one.
         */
        reconfigureTest.simulate(LauncherComp.class);

        /**
         * Verify that the initial cluster got 2 views, one where the joins are added.
         */
        for(int i = 1; i <= SERVERS; i++){
        	String ip = "192.168.0." + i;

        	ArrayList<HashMap<String,Object>> views = res.get(ip+"-views", ArrayList.class);

        	Assert.assertEquals(2, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));

        	ViewWrapper wp1 = fromHashMapToView(views.get(1));

        	Assert.assertEquals(1, wp0.id);
        	Assert.assertEquals(SERVERS, wp0.members.size());

        	Assert.assertEquals(2, wp1.id);
        	Assert.assertEquals(SERVERS + JOINS, wp1.members.size());

        }

        /**
         * Verify that the joined servers got the new view (obv don't got the initial view).
         */
        for(int i = SERVERS+1; i <= SERVERS+JOINS; i++){
        	String ip = "192.168.0." + i;

        	ArrayList<HashMap<String,Object>> views = res.get(ip+"-views", ArrayList.class);

        	Assert.assertEquals(1, views.size());
        	ViewWrapper wp0 = fromHashMapToView(views.get(0));

        	Assert.assertEquals(2, wp0.id);
        	Assert.assertEquals(SERVERS + JOINS, wp0.members.size());
        }
        
        /**
         * Verify that all nodes have consistent store, especially joined servers
         * 
         */
        ArrayList<HashMap<Integer, String>> nodeStores = new ArrayList<>();
        for (int i = 1; i <= SERVERS+JOINS; i++) {
            String ip = "192.168.0." + i;
            nodeStores.add(res.get(ip+"-values", HashMap.class));
        }
        
        HashMap<Integer, String> reference = nodeStores.get(0);
        for (HashMap<Integer, String> nodeStore : nodeStores) {
            Assert.assertTrue(reference.equals(nodeStore));
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
