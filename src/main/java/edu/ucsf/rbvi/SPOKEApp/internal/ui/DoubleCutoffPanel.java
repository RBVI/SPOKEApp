package edu.ucsf.rbvi.spokeApp.internal.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.List;
import java.util.Map;

import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;

import edu.ucsf.rbvi.spokeApp.internal.model.DoubleCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class DoubleCutoffPanel extends JPanel implements ActionListener, ChangeListener { 
	private final SpokeManager manager;
	private final Font smallFont;
	private final Font boldFont;
	protected final Font iconFont;
	private final DoubleCutoff cutoff;

	public DoubleCutoffPanel(final SpokeManager manager, final DoubleCutoff cutoff) {
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

		SpinnerModel model = new SpinnerNumberModel(cutoff.getValue().doubleValue(), cutoff.getMin(), cutoff.getMax(), cutoff.getStep());
		JSpinner spinner = new JSpinner(model);
		spinner.addChangeListener(this);
		add(spinner, c.right().insets(0,20,0,0).expandHoriz());
	}

	// TODO: need to save as property
	public void actionPerformed(ActionEvent e) {
		JCheckBox box = (JCheckBox)e.getSource();
		cutoff.setActive(box.isSelected());
		manager.updateCutoffProperty(cutoff);
	}

	// TODO: need to save as property
	public void stateChanged(ChangeEvent e) {
		JSpinner spinner = (JSpinner)e.getSource();
		SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
		Number v = model.getNumber();
		cutoff.setValue(v.doubleValue());
		manager.updateCutoffProperty(cutoff);
	}

}
