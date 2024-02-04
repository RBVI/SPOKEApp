package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;

public class NeighborhoodTaskFactory extends AbstractTaskFactory {
	final SpokeManager manager;

	public NeighborhoodTaskFactory(final SpokeManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		SpokeNetwork spokeNet = new SpokeNetwork(manager);
		return new TaskIterator(new LoadNeighborhoodCommand(spokeNet, "Spoke"));
	}

	public boolean isReady() {
		return true;
	}
}
