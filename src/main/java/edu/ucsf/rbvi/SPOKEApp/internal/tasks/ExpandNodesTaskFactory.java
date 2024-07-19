package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ExpandNodesTaskFactory extends AbstractTaskFactory {
	final SpokeManager manager;

	public ExpandNodesTaskFactory(final SpokeManager manager) { 
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ExpandNodes(manager));
	}

	public boolean isReady() {
		if (manager.getSpokeNetwork(manager.getCurrentNetwork()) != null)
			return true;
		return false;
	}
}
