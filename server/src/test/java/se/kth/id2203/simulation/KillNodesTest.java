package se.kth.id2203.simulation;

import se.kth.id2203.simulation.scenario.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;


public class KillNodesTest {

	
	public static void main(String[] args) {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario killNodesScenario = ScenarioGen.killNodes(3);
        killNodesScenario.simulate(LauncherComp.class);
    }
}
