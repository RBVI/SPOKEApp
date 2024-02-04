package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;

public class MetagraphTaskFactory extends AbstractTaskFactory {
	final SpokeManager manager;
	String netName;

	public MetagraphTaskFactory(SpokeManager manager, final String netName) {
		this.manager = manager;
		this.netName = netName;
	}

	public TaskIterator createTaskIterator() {
		SpokeNetwork spokeNet = new SpokeNetwork(manager);
		if (spokeNet.getNetwork() == null) {
			return new TaskIterator(new Metagraph(spokeNet, netName));
		}
		return null;
	}

	public boolean isReady() {
		return true;
	}
}
