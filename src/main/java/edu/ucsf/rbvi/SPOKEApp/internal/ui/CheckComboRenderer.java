package edu.ucsf.rbvi.spokeApp.internal.ui;

import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

class CheckComboRenderer implements ListCellRenderer {
	JCheckBox checkBox;

	public CheckComboRenderer() {
		checkBox = new JCheckBox();
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		CheckComboStore store = (CheckComboStore) value;
		checkBox.setText(store.name);
		checkBox.setSelected((Boolean) store.state);
		return checkBox;
	}
}

