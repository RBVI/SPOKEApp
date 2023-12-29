package edu.ucsf.rbvi.spokeApp.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.spokeApp.internal.utils.ModelUtils;

public class DoubleCutoff extends Cutoff {
	double min;
	double max;
	double value;
	double step;

	public DoubleCutoff (String name, String label, double min, double max, double value, double step) {
		super(name, label, null, true, CUTOFF_TYPE.FLOAT);
		this.min = min;
		this.max = max;
		this.value = value;
		this.step = step;
	}

	public DoubleCutoff (String name, JSONObject cutoff) {
		super(name, cutoff);

		JSONArray rangeList =  (JSONArray) cutoff.get("range");
		if (rangeList != null && rangeList.size() > 0 ) {
			List<Number> range = (List<Number>)ModelUtils.makeList((Object)rangeList, Number.class);
			min = range.get(0).doubleValue();
			max = range.get(1).doubleValue();
		}
		value = ((Number)cutoff.get("default")).doubleValue();

		Number nstep = (Number) cutoff.get("step");
		if (nstep != null)
			step = nstep.doubleValue();
		else
			step = (max-min)/100.0;
	}

	public double getValue() { return value; }
	public void setValue(double value) { this.value = value; }

	public double getMin() { return min; }
	public double getMax() { return max; }
	public double getStep() { return step; }
}
