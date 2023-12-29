package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ExpandNodeTaskFactory extends AbstractNodeViewTaskFactory {
	final SpokeManager manager;

	public ExpandNodeTaskFactory(final SpokeManager manager) { 
		this.manager = manager;
	}

	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		return new TaskIterator(new ExpandNode(manager, networkView, nodeView.getModel()));
	}

	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		if (manager.getSpokeNetwork(networkView.getModel()) != null)
			return true;
		return false;
	}
}
