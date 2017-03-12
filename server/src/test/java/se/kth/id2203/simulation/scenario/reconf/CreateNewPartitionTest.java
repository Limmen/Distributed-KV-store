package se.kth.id2203.simulation.scenario.reconf;

import org.junit.Assert;
import org.junit.Test;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test that new partition can be created dynamically upon join requests when the existing partitions are full.
 * Also test that keys are handed over correctly
 */
public class CreateNewPartitionTest {

    private static final int SERVERS = 3;
    private static final int REPLICATION_DEGREE = 2;
    private static final int JOINS = 2;
    private static final int KEY_RANGE = 50;

    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void createNewPartitionTest() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario reconfigureTest = ScenarioGen.reconfTest(SERVERS, REPLICATION_DEGREE, JOINS, KEY_RANGE);
        res.put("trace", new ConcurrentLinkedQueue<>());

        /**
         * Boot up SERVERS in a cluster and a single replication group, also boot up a client which will send 2 Put requests
         * one with key 49(hash(1)) and one with key 1572 (hash(15)) then boot up JOINS servers which will request to join.
         * Since Servers=3 and quorum=2, there is not space to join the existing partition but since there are 2 join-requests
         * they can form a new partition which satisfies the replication degree. Upon creation of the new partition with keyspace=50
         * it means that the first partition is responsible for keys 0-49 and the second is responsible for 50-... so the key
         * 1572 should be handed over to the new partition.
         */
        reconfigureTest.simulate(LauncherComp.class);

        /**
         * Verify that the first partition has the right keys and the correct global view
         */
        for (int i = 1; i <= SERVERS; i++) {
            String ip = "192.168.0." + i;
            ArrayList<Map> globalViews = res.get(ip + "-globalviews", ArrayList.class);
            Assert.assertEquals(2, globalViews.size());
            Map gw1 = globalViews.get(0);
            Assert.assertEquals(1, gw1.keySet().size()); //1 partition only in first view
            Map gw2 = globalViews.get(1);
            Assert.assertEquals(2, gw2.keySet().size()); //2 partitions in second view

            HashMap<Integer, String> store = res.get(ip + "-values", HashMap.class);
            for (int key : store.keySet()) {
                Assert.assertTrue(key < KEY_RANGE); //Higher keys should have been handed over
            }
        }

        /**
         * Verify that the second partition has the right keys and the correct global view
         */
        for (int i = SERVERS + 1; i <= SERVERS + JOINS; i++) {
            String ip = "192.168.0." + i;
            ArrayList<Map> globalViews = res.get(ip + "-globalviews", ArrayList.class);
            Map gw1 = globalViews.get(1);
            Assert.assertEquals(2, gw1.keySet().size()); //2 partitions

            HashMap<Integer, String> store = res.get(ip + "-values", HashMap.class);
            Assert.assertEquals(1, store.keySet().size()); //One of the keys put by the client should have been handed over upon joining
            for (int key : store.keySet()) {
                Assert.assertTrue(key >= KEY_RANGE); //Lower keys should remain at first partition
            }
        }

    }
}
