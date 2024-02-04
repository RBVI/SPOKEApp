package edu.ucsf.rbvi.spokeApp.internal.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.List;
import java.util.Map;

import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;

import edu.ucsf.rbvi.spokeApp.internal.model.ChoiceCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ChoiceCutoffPanel extends JPanel implements ActionListener { 
	private final SpokeManager manager;
	private final Font smallFont;
	private final Font boldFont;
	protected final Font iconFont;
	private final ChoiceCutoff cutoff;
	private CheckComboStore[] stores;

	public ChoiceCutoffPanel(final SpokeManager manager, final ChoiceCutoff cutoff) {
		super(new GridBagLayout());
		this.manager = manager;
		this.cutoff = cutoff;

		smallFont = getFont().deriveFont(LookAndFeelUtil.getSmallFontSize());
		boldFont = smallFont.deriveFont(Font.BOLD);
		IconManager iconManager = manager.getService(IconManager.class);
    iconFont = iconManager.getIconFont(17.0f);

		setLayout(new GridBagLayout());

		EasyGBC c = new EasyGBC();
		String label = cutoff.getLabel();
		String description = cutoff.getDescription();
		boolean active = cutoff.isActive();
		JCheckBox box = new JCheckBox(label, active);
		box.addActionListener(this);
		add(box, c.anchor("west"));

		// This will be a JComboBox
		List<String> range = cutoff.getRange();
		if (range != null && range.size() > 0) {
			stores = new CheckComboStore[range.size()];

			int index = 0;
			for (String r: range) 
				stores[index++] = new CheckComboStore(r, r, cutoff.isSelected(r), false);

			JComboBox cbox = new JComboBox(stores);
			cbox.setRenderer(new CheckComboRenderer());
			cbox.addActionListener(this);
			add(cbox, c.right().expandHoriz());
		} else {
			add(new JLabel(), c.right().expandHoriz());
		}
	}

	// TODO: need to save as property
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JComboBox) {
			JComboBox cb = (JComboBox) e.getSource();
			CheckComboStore store = (CheckComboStore) cb.getSelectedItem();
			CheckComboRenderer ccr = (CheckComboRenderer) cb.getRenderer();
			ccr.checkBox.setSelected((store.state = !store.state));
			cutoff.select(store.name, store.state);
		} else if (e.getSource() instanceof JCheckBox) {
			JCheckBox cb = (JCheckBox) e.getSource();
			cutoff.setActive(cb.isSelected());
		}
		manager.updateCutoffProperty(cutoff);
	}

}
