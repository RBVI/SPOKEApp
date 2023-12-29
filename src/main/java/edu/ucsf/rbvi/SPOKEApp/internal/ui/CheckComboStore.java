package edu.ucsf.rbvi.spokeApp.internal.ui;

class CheckComboStore {
	String id;
	String name;
	Boolean state;
	boolean isNode;

	public CheckComboStore(String id, String name, Boolean state, boolean isNode) {
		this.id = id;
		this.name = name;
		this.state = state;
		this.isNode = isNode;
	}
}
