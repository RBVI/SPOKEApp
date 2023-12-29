package edu.ucsf.rbvi.spokeApp.internal.tasks;

import static edu.ucsf.rbvi.spokeApp.internal.utils.IconUtils.SPOKE_COLORS;
import static edu.ucsf.rbvi.spokeApp.internal.utils.IconUtils.SPOKE_LAYERS;
import static edu.ucsf.rbvi.spokeApp.internal.utils.IconUtils.getIconFont;

import java.awt.Dialog;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;
import edu.ucsf.rbvi.spokeApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.spokeApp.internal.ui.SearchQueryComponent;
import edu.ucsf.rbvi.spokeApp.internal.utils.IconUtils;
import edu.ucsf.rbvi.spokeApp.internal.utils.TextIcon;
import edu.ucsf.rbvi.spokeApp.internal.utils.TextUtils;

public class SpokeSearchTaskFactory extends AbstractNetworkSearchTaskFactory implements TaskObserver {
	SpokeManager manager = null;
	static String SPOKE_ID = "edu.ucsf.rbvi.spoke";
	static String SPOKE_URL = "http://spoke.rbvi.ucsf.edu";
	static String SPOKE_NAME = "SPOKE query";
	static String SPOKE_DESC = "Search the SPOKE knowledge graph ";
	static String SPOKE_DESC_LONG = "<html>The query retrieves a SPOKE neighborhood for the node. <br />"
										+ "SPOKE is a knowledge graph of biomedical knowledge.</html>";

	private SpokeNetwork spokeNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private SearchQueryComponent queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	// private static final Icon icon = new TextIcon(SPOKE_LAYERS, getIconFont(32.0f), SPOKE_COLORS, 36, 36);
	private static final Icon icon = IconUtils.getImageIcon();

	private static URL spokeURL() {
		try {
			return new URL(SPOKE_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public SpokeSearchTaskFactory(SpokeManager manager) {
		super(SPOKE_ID, SPOKE_NAME, SPOKE_DESC, icon, SpokeSearchTaskFactory.spokeURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		if (queryComponent.getQueryText() != null && queryComponent.getQueryText().length() > 0)
			return true; 
		return false;
	}

	public TaskIterator createTaskIterator() {
		String query = queryComponent.getQuery();

		// Parse out the components -- for now, just split on ":"
		String[] parts = query.split(":");
		if (parts.length != 3) {
			System.out.println("Error -- need type, attribute, and query");
			return null;
		}

		spokeNetwork = new SpokeNetwork(manager);
		return new TaskIterator(new LoadNeighborhood(spokeNetwork, parts[0], parts[1], parts[2], null));
	}

	@Override
	public String getName() { return SPOKE_NAME; }

	@Override
	public String getId() { return SPOKE_ID; }

	@Override
	public String getDescription() {
		return SPOKE_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return SpokeSearchTaskFactory.spokeURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager);
		return optionsPanel;
	}

	@Override
	public JComponent getQueryComponent() {
		if (queryComponent == null)
			queryComponent = new SearchQueryComponent(manager);
		return queryComponent;
	}

	@Override
	public TaskObserver getTaskObserver() { return this; }

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}


	@Override
	public void taskFinished(ObservableTask task) {
	}

}
