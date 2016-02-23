package example;

import org.rspql.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SP;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestRSPQLSPIN {
	public static void main(String[] args) {
		// Register the parser
		ParserRSPQL.register();

		String q = ""
				+ "PREFIX : <http://test#>\n"
				+ "CONSTRUCT {\n"
				+ "   GRAPH ?g { :a :b ?c ; :c :d .} . :a :b ?c ; :c :d .\n"
				+ "   ?g :detectedAt ?t\n"
				+ "}\n"
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT10s]"
				+ "WHERE {\n"
				+ "   WINDOW :w {\n"
				+ "      GRAPH ?g { :a :b ?c }\n"
				+ "      ?g :timestamp ?t"
				+ "   }"
				+ "}";
		q = ""
				+ "PREFIX : <http://test#> \n"
				+ "CONSTRUCT { GRAPH :g1 { :a :b :c } GRAPH :g2 { :a :b :c } } \n"
				//+ "FROM NAMED WINDOW :w1 ON :s1 [FROM NOW-PT1h TO NOW-?b STEP ?s] \n"
				+ "WHERE {\n"
				+ "   :a :b :c .\n"
				+ "}";
		
		String qBig = ""
				+ "PREFIX : <http://test#> \n"
				+ "REGISTER STREAM :s1 AS \n"
				+ "CONSTRUCT ISTREAM { ?a :foo ?c } \n"
				+ "FROM NAMED WINDOW :w1 ON :s1 [RANGE PT1m10s STEP PT30s] \n"
				+ "FROM NAMED WINDOW :w2 ON :s2 [RANGE PT1m10s] \n"
				+ "FROM NAMED WINDOW :w3 ON ?s [RANGE ?range STEP ?step] \n"
				+ "FROM NAMED WINDOW :w4 ON :s1 [ITEM 10 STEP 5] \n"
				+ "FROM NAMED WINDOW :w5 ON :s2 [ITEM 10] \n"
				+ "FROM NAMED WINDOW :w6 ON ?s [ITEM ?r STEP ?s] \n"
				+ "FROM NAMED :g1 \n"
				+ "FROM NAMED :g2 \n"
				+ "FROM :default \n"
				+ "WHERE { \n"
				+ "   {WINDOW :w1 { \n"
				+ "      ?a :foo ?c . \n"
				+ "      GRAPH ?g { ?a :foo :Bar } "
				+ "      FILTER regex(str(?a), 'abc', 'i')\n"
				+ "   }}"
				+ "   UNION \n"
				+ "   {WINDOW :w2 { \n"
				+ "      ?a :foo ?c . \n"
				+ "      GRAPH :g { ?a ?b ?c } \n"
				+ "      FILTER (?b != :foo)\n"
				+ "   }}"
				+ "   UNION \n"
				+ "   {GRAPH :g1 { ?a :foo :Bar }}\n"
				+ "}\n"
				+ "LIMIT 10";
		
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSyntax);
		
		// Initialize SPIN system functions and templates
		SPINModuleRegistry.get().init();
		
		Model model = ModelFactory.createDefaultModel();
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);
		
		model.setNsPrefix("", "http://test#");
		model.setNsPrefix("sp", SP.NS);
		model.setNsPrefix("rsp", SP.RSP);
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		model.write(System.out, "TTL");
		
		System.out.println(spinQuery);
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		Query reparsedQuery = ARQFactory.get().createQuery(spinQuery);
		reparsedQuery.setPrefixMapping(query.getPrefixMapping());
		System.err.println(reparsedQuery);
	}
}
