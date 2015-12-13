/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspql.syntax;

public class ElementPhysicalWindow extends ElementNamedWindow {
	private Object size;
	private Object step;

	/**
	 * Create a new physical window.
	 * 
	 * @param windowIri
	 * @param streamIri
	 * @param range
	 * @param step
	 */
	public ElementPhysicalWindow(String windowIri, Object streamIri, Object size, Object step) {
		super(windowIri, streamIri);
		this.size = size;
		this.step = step;
	}

	/**
	 * Get the physical window size. 
	 * @return size is a varible or an integer
	 */
	public Object getSize() {
		return size;
	}

	/**
	 * Get the physical window step.
	 * @return step is a variable, an integer, or null
	 */
	public Object getStep() {
		return step;
	}
}
