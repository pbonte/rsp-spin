/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspql.syntax;

public class ElementLogicalWindow extends ElementNamedWindow {
	private Object range;
	private Object step;

	/**
	 * Create a new logical window.
	 * 
	 * @param windowIri
	 * @param streamIri
	 * @param range
	 * @param step
	 */
	public ElementLogicalWindow(String windowIri, Object streamIri, Object range, Object step) {
		super(windowIri, streamIri);
		this.range = range;
		this.step = step;
	}

	/**
	 * Get the logical window range. 
	 * @return range is a varible or a duration
	 */
	public Object getRange() {
		return range;
	}

	/**
	 * Get the logical window step.
	 * @return step is a variable, a duration, or null
	 */
	public Object getStep() {
		return step;
	}
}
