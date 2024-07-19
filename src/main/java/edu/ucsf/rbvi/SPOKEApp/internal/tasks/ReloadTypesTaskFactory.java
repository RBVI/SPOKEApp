package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ReloadTypesTaskFactory extends AbstractTaskFactory {
	final SpokeManager manager;

	public ReloadTypesTaskFactory(SpokeManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ReloadTypes(manager));
	}

	public boolean isReady() {
		return true;
	}
}
