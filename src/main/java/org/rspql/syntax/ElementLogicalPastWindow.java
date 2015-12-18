/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspql.syntax;

public class ElementLogicalPastWindow extends ElementLogicalWindow {
	private Object from;
	private Object to;

	/**
	 * Create a logical past window.
	 * 
	 * @param windowIri
	 * @param streamIri
	 * @param from
	 * @param to
	 * @param step
	 */
	public ElementLogicalPastWindow(String windowIri, Object streamIri, Object from, Object to, Object step) {
		super(windowIri, streamIri, null, step);
		this.from = from;
		this.to = to;
	}

	/**
	 * Get logical window start time.
	 * 
	 * @return
	 */
	public Object getFrom() {
		return from;
	}

	/**
	 * Get logical window end time.
	 * 
	 * @return
	 */
	public Object getTo() {
		return to;
	}

}
