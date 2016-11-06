package org.rspspin.lang.sparqlstream;

import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Template;

public class FmtTemplate extends org.apache.jena.sparql.serializer.FmtTemplate {
	static final int INDENT = 2;

	public FmtTemplate(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	@Override
	public void format(Template template) {
		out.print("{");
		out.incIndent(INDENT);
		out.pad();

		List<Quad> quads = template.getQuads();
		boolean quadInOutput = false;
		for (Quad quad : quads) {
			if (!quad.isDefaultGraph())
				quadInOutput = true;
			BasicPattern bgp = new BasicPattern();
			bgp.add(quad.asTriple());
			out.newline();
			formatTriples(bgp);
		}
		out.println();
		out.decIndent(INDENT);
		out.print("}");
		out.decIndent(INDENT);
		out.println();

		if (quadInOutput) {
			System.err.println("WARNING: SPARQLStream does not support quads in construct results. "
					+ "Triples are collapsed into the default graph.");
		}

	}
}
