package edu.ucsf.rbvi.spokeApp.internal.model;

//import java.util.HashSet;
//import java.util.Set;

import org.cytoscape.model.CyNetwork;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class SpokeNetwork {
	SpokeManager manager;
	CyNetwork network;

	public SpokeNetwork(SpokeManager manager) {
		this.manager = manager;
	}

	public void setNetwork(CyNetwork network) {
    this.network = network;
	}

	public CyNetwork getNetwork() { return network; }

	public SpokeManager getManager() { return manager; }

}
