package se.kth.id2203.simulation.scenario.reconf;

import org.junit.Assert;
import org.junit.Test;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test that if a partition is crashed this is detected by the other partitions who update their global view (lookuptables).
 */
public class CrashPartitionTest {

    private static final int SERVERS = 6;
    private static final int REPLICATION_DEGREE = 2;
    private static final int CRASHES = 3;
    private static final int KEY_RANGE = 50;

    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();


    @Test
    public void crashPartitionTest() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario reconfigureTest = ScenarioGen.reconfCrashTest(SERVERS, REPLICATION_DEGREE, CRASHES, KEY_RANGE);
        res.put("trace", new ConcurrentLinkedQueue<>());

        /**
         * Boot up SERVERS in a cluster with 2 partitions, then crash the first partition.
         */
        reconfigureTest.simulate(LauncherComp.class);

        /**
         * Verify that the non-crashed partition detected crash of the other partition and updated global view
         */
        for (int i = CRASHES+1; i <= SERVERS; i++) {
            String ip = "192.168.0." + i;
            ArrayList<Map> globalViews = res.get(ip + "-globalviews", ArrayList.class);
            int lastView = globalViews.size()-1;
            Map gw = globalViews.get(lastView);
            Assert.assertEquals(1, gw.keySet().size()); //Crash of partition should have been detected and new global view with only one partition
        }

        /**
         * Verify that it actually was 2 partitions before crash
         */
        for(int i = 1; i <= CRASHES; i++){
            String ip = "192.168.0." + i;
            ArrayList<Map> globalViews = res.get(ip + "-globalviews", ArrayList.class);
            int lastView = globalViews.size()-1;
            Map gw = globalViews.get(lastView);
            Assert.assertEquals(2, gw.keySet().size()); //Global-View before crash had 2 partitions
        }

    }
}
