package edu.ucsf.rbvi.spokeApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.util.swing.LookAndFeelUtil;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.ui.EasyGBC;


public class SearchQueryComponent extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	private JPopupMenu popup = null;
	private static final String DEF_SEARCH_TEXT = "Enter query.      Set node type →";
	private final SpokeManager manager;

	private JComboBox nodeTypeComboBox;
	private JTextField queryField;
	private Font smallFont;

	final int vgap = 1;
	final int hgap = 1;
	final String tooltip;
	Color msgColor;

	public SearchQueryComponent(final SpokeManager manager) {
		super();
		this.manager = manager;
		init();
		tooltip = "Press " + (LookAndFeelUtil.isMac() ? "Command" : "Ctrl") + "+ENTER to run the search";
	}

	void init() {

		msgColor = UIManager.getColor("Label.disabledForeground");
		setMinimumSize(getPreferredSize());
		setBorder(BorderFactory.createEmptyBorder(vgap, hgap, vgap, hgap));
		smallFont = getFont().deriveFont(LookAndFeelUtil.getSmallFontSize());
		setFont(smallFont);
		setLayout(new GridBagLayout());

		EasyGBC c = new EasyGBC();

		List<String> nodeTypes = manager.getNodeTypes();
		Collections.sort(nodeTypes);
		String[] nTypes = nodeTypes.toArray(new String[0]);
		nodeTypeComboBox = new JComboBox(nTypes);
		String defaultType = manager.getDefaultType();
		nodeTypeComboBox.setSelectedItem(defaultType);

		nodeTypeComboBox.setFont(smallFont);

		queryField = new JTextField(16);
		queryField.setFont(smallFont);
		queryField.addActionListener(this);

		add(nodeTypeComboBox, c.insets(0,0,0,0));
		add(queryField, c.right().insets(0,0,0,0));
		

    setToolTipText(tooltip);

		return;
	}

	public String getQueryText() {
		if (queryField == null) return null;
		String terms = queryField.getText();
		// terms = terms.replaceAll("(?m)^\\s*", "");
		// terms = terms.replaceAll("(?m)\\s*$", "");

		return terms;
	}

	public String getQuery() {
		String t = (String)nodeTypeComboBox.getSelectedItem();
		String q = queryField.getText();
		return t+":name:"+q;
	}

	private void updateQueryTextField() {
		// String text = query.stream().collect(Collectors.joining(" "));
		// TODO: truncate the text -- no need for this to be the entire string
		String text = queryField.getText();
		if (text.length() > 30)
			text = text.substring(0, 30)+"...";
		queryField.setText(text);
  }

	private void fireQueryChanged() {
		firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
	}

	public void actionPerformed(ActionEvent e) {
		// Get the query
		String queryText = queryField.getText();

		// Get the type
		String nodeType = nodeTypeComboBox.getSelectedItem().toString();

		// Set our property
		manager.setDefaultType(nodeType);

		// Send it off to the be searched
		List<String> possibleMatches = manager.getSearchResults(nodeType, queryText+"*");
		if (possibleMatches.size() == 1) {
			String match = possibleMatches.get(0);
			String name = match.split("\t")[0];
			queryField.setText(name);
			fireQueryChanged();
			return;
		}

		// Popup a list
		JPopupMenu searchResults = new JPopupMenu("Choose");
		for (String match: possibleMatches) {
			searchResults.add(new MyAction(match));
		}
		searchResults.setPopupSize(12*64, 14*possibleMatches.size());
		searchResults.setFont(smallFont);
		searchResults.show(queryField, 0, 0);
	}

	public class MyAction extends AbstractAction {
		private String text;

		public MyAction(String text) {
			super(text);
			this.text = text;
		}
		public void actionPerformed(ActionEvent e) {
			String name = text.split("\t")[0];
			queryField.setText(name);
			fireQueryChanged();
		}
	}

}
