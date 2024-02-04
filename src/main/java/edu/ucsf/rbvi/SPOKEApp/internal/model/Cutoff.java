package edu.ucsf.rbvi.spokeApp.internal.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class Cutoff {
	public enum CUTOFF_TYPE {
		CHOICE, FLOAT, NONE
	}

	protected CUTOFF_TYPE type;
	protected String label;
	protected String description;
	protected boolean active;
	protected String name;

	public Cutoff(String name, String label, String description, boolean active, CUTOFF_TYPE type) {
		this.name = name;
		this.label = label;
		this.description = description;
		this.active = active;
		this.type = type;
	}

	public Cutoff(String name, JSONObject cutoff) {
		this.name = name;
		this.type = parseType(cutoff);
		this.label = (String)cutoff.get("label");
		this.description = (String)cutoff.get("description");
		this.active = (Boolean)cutoff.get("active");
	}

	public CUTOFF_TYPE getType() { return type; }
	public String getName() { return name; }
	public String getLabel() { return label; }
	public String getDescription() { return description; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }

	public String quote(String q) { return "\""+q+"\""; }
	public String getStringType() {
		switch(type) {
			case CHOICE:
				return "choice";
			case FLOAT:
				return "float";
		}
		return "none";
	}
	public String getStringActive() { 
		if (active) 
			return "true";
		else
			return "false";
	}

	public abstract void setValue(Object value);
	public abstract Object getValue();
	public abstract void setState(JSONObject json);
	public abstract String getProperty();

	public static CUTOFF_TYPE parseType(JSONObject cutoff) {
		String stringType = (String)cutoff.get("type");
		if (stringType.equals("choice")) return CUTOFF_TYPE.CHOICE;
		if (stringType.equals("float")) return CUTOFF_TYPE.FLOAT;
		return CUTOFF_TYPE.NONE;
	}

	public static Cutoff createCutoff(String name, JSONObject cutoff) {
		CUTOFF_TYPE t = parseType(cutoff);
		switch (t) {
			case CHOICE:
				return new ChoiceCutoff(name, cutoff);
			case FLOAT:
				return new DoubleCutoff(name, cutoff);
		}
		return null;
	}

}

