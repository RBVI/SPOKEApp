package edu.ucsf.rbvi.spokeApp.internal.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;

import edu.ucsf.rbvi.spokeApp.internal.model.ChoiceCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.DoubleCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.Cutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

// TODO: [Optional] Improve non-gui mode
public class SearchOptionsPanel extends JPanel implements ListSelectionListener { 
	final SpokeManager manager;

	// JComboBox<CheckComboStore> nodeComboBox;
	// JComboBox<CheckComboStore> edgeComboBox;

	private final Font italics;
	private final Font smallFont;
	private final Font boldFont;
	protected final Font iconFont;

	private Map<String, String> nameToTypeMap = new HashMap<>();


	public SearchOptionsPanel(final SpokeManager manager) {
		super(new GridBagLayout());
		this.manager = manager;

		smallFont = getFont().deriveFont(LookAndFeelUtil.getSmallFontSize());
		boldFont = smallFont.deriveFont(Font.BOLD);
		italics = getFont().deriveFont(Font.ITALIC);
		IconManager iconManager = manager.getService(IconManager.class);
    iconFont = iconManager.getIconFont(17.0f);

		initOptions();
		setPreferredSize(new Dimension(700,800));
	}

	private void initOptions() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		JScrollPane pane = new JScrollPane(mainPanel);
		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		EasyGBC c1 = new EasyGBC();
		add(pane, c1.anchor("northwest").expandBoth());

		// setPreferredSize(new Dimension(700,200));
		EasyGBC c = new EasyGBC();

		mainPanel.add(createNodeAndEdgeFilters(), c.anchor("northwest").expandHoriz().insets(0,0,0,0));

		// Create Filters Panels
		CollapsablePanel nodeFilterPanel = createNodeFilterPanel();
		CollapsablePanel edgeFilterPanel = createEdgeFilterPanel();
		CollapsablePanel limitsPanel = createLimitsPanel();

		mainPanel.add(nodeFilterPanel, c.down().expandHoriz().insets(10,10,0,0));
		mainPanel.add(edgeFilterPanel, c.down().expandHoriz().insets(10,10,0,0));
		mainPanel.add(limitsPanel, c.down().expandHoriz().insets(10,10,0,0));
		
		// Fill out
		JLabel filler = new JLabel();
		mainPanel.add(filler, c.down().expandBoth());
	}

	private JPanel createNodeAndEdgeFilters() {
		JPanel nodeAndEdgeFilterPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		List<String> nodeTypes = manager.getNodeTypes();

		List<String> edgeTypes = manager.getEdgeTypes();

		// We do this so our list is in alphabetical order
		for (String type: edgeTypes) {
			nameToTypeMap.put(manager.getEdgeName(type), type);
		}
		List<String> edgeNames = manager.getEdgeNames();

		JLabel nodeBoxLabel = new JLabel("Node Types:");
		nodeBoxLabel.setFont(boldFont);
		JPanel nodeList = createCheckBoxList(nodeTypes, true);

		JLabel edgeBoxLabel = new JLabel("Edge Types:");
		edgeBoxLabel.setFont(boldFont);
		JPanel edgeList = createCheckBoxList(edgeNames, false);

		nodeAndEdgeFilterPanel.add(nodeBoxLabel, c.anchor("northwest").insets(10,10,0,0));
		nodeAndEdgeFilterPanel.add(edgeBoxLabel, c.right().expandHoriz().insets(10,10,0,0));
		nodeAndEdgeFilterPanel.add(nodeList, c.down().insets(0,10,0,0));
		nodeAndEdgeFilterPanel.add(edgeList, c.right().expandHoriz().insets(0,10,0,10));

		return nodeAndEdgeFilterPanel;
	}

	private JPanel createCheckBoxList(List<String> types, boolean isNode) {
		JPanel panel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		CheckComboStore[] stores = new CheckComboStore[types.size()];
		int index = 0;
		for (String type: types) {
			if (isNode)
				stores[index++] = new CheckComboStore(type, type, !manager.getNodeSkip(type), isNode);
			else {
				String realType = nameToTypeMap.get(type);
				stores[index++] = new CheckComboStore(realType, type, !manager.getEdgeSkip(realType), isNode);
			}
		}

		JList<CheckComboStore> box = new JList<>(stores);
		JScrollPane scrollpane = new JScrollPane(box);
		box.setVisibleRowCount(20);
		box.addListSelectionListener(this);
		box.setFont(smallFont);
		box.setCellRenderer(new CheckComboRenderer());
		panel.add(scrollpane, c.anchor("northwest").expandHoriz().spanHoriz(2));
		JCheckBox all = new JCheckBox("Check all");
		all.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				box.clearSelection();
				box.setSelectionInterval(0, types.size()-1);
				// for (int i = 0; i < types.size(); i++) {
					// CheckComboStore sel = (CheckComboStore) box.getModel().getElementAt(i);
					// sel.state = true;
					// box.setSelectedIndex(i);
				// }
				all.setSelected(false);
			}
		});
		JCheckBox clear = new JCheckBox("Uncheck all");
		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				box.setSelectionInterval(0, types.size()-1);
				// for (int i = 0; i < types.size(); i++) {
				// 	CheckComboStore sel = (CheckComboStore) box.getModel().getElementAt(i);
				// 	sel.state = false;
				// }
				box.clearSelection();
				clear.setSelected(false);
			}
		});
		panel.add(all, c.noSpan().anchor("west").down());
		panel.add(clear, c.right());
		return panel;
	}

	private CollapsablePanel createNodeFilterPanel() {
		JPanel nodeFilterPanel = createCutoffs(manager.getNodeCutoffs());
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Node filters", nodeFilterPanel, true, 10);

		return collapsablePanel;
	}

	private CollapsablePanel createEdgeFilterPanel() {
		JPanel edgeFilterPanel = createCutoffs(manager.getEdgeCutoffs());
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Edge filters", edgeFilterPanel, true, 10);

		return collapsablePanel;
	}

	private CollapsablePanel createLimitsPanel() {
		JPanel limitsPanel = createCutoffs(manager.getLimits());
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Limits", limitsPanel, true, 10);

		return collapsablePanel;
	}

	public void valueChanged(ListSelectionEvent e) {
		// System.out.println(e.toString());
		JList cb = (JList) e.getSource();
		if (e.getValueIsAdjusting()) return;
		for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
			CheckComboStore store = (CheckComboStore) cb.getModel().getElementAt(i);
			store.state = cb.isSelectedIndex(i);
			if (store.isNode) {
				manager.setNodeSkip(store.id, !store.state);
			} else {
				manager.setEdgeSkip(store.id, !store.state);
			}
		}
	}

  public void cancel() {
    ((Window)getRootPane().getParent()).dispose();
  }

	private JPanel createCutoffs(Map<String, Cutoff> cutoffs) {
		JPanel panel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		for (String key: cutoffs.keySet()) {
			Cutoff cutoff = cutoffs.get(key);
			JPanel cutoffPanel = createCutoff(cutoff);
			panel.add(cutoffPanel, c.down().anchor("west"));
			panel.add(new JLabel(), c.right().expandHoriz());
		}
		return panel;
	}

	private JPanel createCutoff(Cutoff cut) {
		if (cut.getType() == Cutoff.CUTOFF_TYPE.CHOICE) {
			return new ChoiceCutoffPanel(manager, (ChoiceCutoff)cut);
		} else {
			return new DoubleCutoffPanel(manager, (DoubleCutoff)cut);
		}
	}

}
