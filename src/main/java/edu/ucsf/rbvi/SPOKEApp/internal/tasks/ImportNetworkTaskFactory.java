package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;

public class ImportNetworkTaskFactory extends AbstractTaskFactory {
	final SpokeNetwork spokeNet;
	final String nodeType;
	final String attribute;
	final String query;
	String netName;

	public ImportNetworkTaskFactory(final SpokeNetwork spokeNet, 
	                                final String nodeType,
	                                final String attribute,
	                                final String query,
																	final String netName) {
		this.spokeNet = spokeNet;
		this.netName = netName;
		this.nodeType = nodeType;
		this.attribute = attribute;
		this.query = query;
	}

	public TaskIterator createTaskIterator() {
		if (spokeNet.getNetwork() == null) {
			return new TaskIterator(new LoadNeighborhood(spokeNet, nodeType, attribute, query, netName));
		}
		return null;
	}

	public boolean isReady() {
		return true;
	}
}
