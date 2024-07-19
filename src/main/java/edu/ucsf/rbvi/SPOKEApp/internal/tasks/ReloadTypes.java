package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ReloadTypes extends AbstractTask {
	final SpokeManager manager;

	public ReloadTypes(final SpokeManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		manager.getTypes();
	}

	@ProvidesTitle
	public String getTitle() { return "Loading neighborhood"; }
}
