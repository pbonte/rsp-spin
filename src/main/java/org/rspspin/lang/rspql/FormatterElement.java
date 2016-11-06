package org.rspspin.lang.rspql;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.rspspin.syntax.ElementWindowGraph;

public class FormatterElement extends org.apache.jena.sparql.serializer.FormatterElement {

	public FormatterElement(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	@Override
	public void visit(ElementWindowGraph el) {
		visitNodePattern("WINDOW", el.getWindowNameNode(), el.getElement());
	}

}
