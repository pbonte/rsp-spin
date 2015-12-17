/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspql.syntax;

import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class ElementNamedWindow extends Element implements Comparable<ElementNamedWindow> {
	private String windowIri;
	private Object streamIri;

	public ElementNamedWindow(String windowIri, Object streamIri) {
		this.windowIri = windowIri;
		this.streamIri = streamIri;
	}

	public String getWindowIri() {
		return windowIri;
	}

	public Object getStream() {
		return streamIri;
	}

	@Override
	public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
		if (!(el2 instanceof ElementNamedWindow))
			return false;
		ElementNamedWindow f2 = (ElementNamedWindow) el2;
		if (!this.getWindowIri().equals(f2.getWindowIri()))
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return windowIri.hashCode() ^ streamIri.hashCode();
	}

	@Override
	public void visit(ElementVisitor v) {
	}

	@Override
	public int compareTo(ElementNamedWindow window) {
		// Sort by window iri
		int i = windowIri.compareTo(window.getWindowIri());
		// Sort by stream
		i = i == 0 ? streamIri.toString().compareTo(window.getStream().toString()) : i;
		// Sort by window type
		i = i == 0 ? getClass().getName().compareTo(window.getClass().getName()) : i;
		
		return i;
	}
}
