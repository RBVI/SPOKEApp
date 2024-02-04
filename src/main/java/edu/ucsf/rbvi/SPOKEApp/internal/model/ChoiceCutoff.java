package edu.ucsf.rbvi.spokeApp.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.spokeApp.internal.utils.ModelUtils;

public class ChoiceCutoff extends Cutoff {
	List<String> range;
	Map<String, Boolean> selected;

	public ChoiceCutoff (String name, JSONObject cutoff) {
		super(name, cutoff);

		JSONArray value =  (JSONArray) cutoff.get("range");
		if (value.size() > 0 ) {
			range = (List<String>)ModelUtils.makeList((Object)value);
		} else {
			range = new ArrayList<>();
		}

		selected = new HashMap<>();
		for (String v: range) {
			selected.put(v, Boolean.FALSE);
		}

		JSONArray dvalue =  (JSONArray) cutoff.get("default");
		if (dvalue.size() > 0 ) {
			List<String> def = (List<String>)ModelUtils.makeList((Object)dvalue);
			for (String d: def) {
				selected.put(d, Boolean.TRUE);
			}
		}
	}

	public List<String> getSelected() {
		List<String> values = new ArrayList<>();
		for (String key: selected.keySet()) {
			if (isSelected(key))
				values.add(key);
		}
		return values;
	}

	public void setValue(Object list) {
		List<String> value;
		if (list instanceof List)
			value = (List<String>)list;
		else
			return;

		for (String v: value) select(v, true);
	}

	public Object getValue() {
		List<String> values = new ArrayList<>();
		for (String v: selected.keySet()) {
			if (selected.get(v))
				values.add(v);
		}
		return values;
	}

	public List<String> getRange() { return range; }

	public boolean isSelected(String key) {
		return selected.get(key);
	}

	public void select(String key, boolean sel) {
		selected.put(key, sel);
	}

	public void setState(JSONObject obj) {
		super.setActive((Boolean)obj.get("active"));
		setValue(ModelUtils.jsonToList((JSONArray)obj.get(name)));
	}

	public String getStringDefault() {
		String list = "[";
		for (String r: selected.keySet()) {
			if (selected.get(r))
				list += super.quote(r)+",";
		}
		return list.substring(0,list.length()-1)+"]";
	}

	public String getProperty() {
		String json = "{"+super.quote(name)+": "+getStringDefault()+","+super.quote("active")+":"+super.isActive()+"}";
		return json;
	}

}
