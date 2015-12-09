package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.print.StringPrintContext;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

public class Demonstration {
	public static void main(String[] args) {
		//if(true) return;
		// Prefixes
		PrefixMapping nsMap = new PrefixMappingImpl();
		nsMap.setNsPrefix("", "http://example.org/");
		nsMap.setNsPrefix("rsp", "https://w3id.org/rsp/spin#");
		nsMap.setNsPrefix("sp", "http://spinrdf.org/sp#");
		nsMap.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		nsMap.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				
		String[] queries = getTestQueries();
		int i = 0;
		for(String query : queries){
			// Model
			Model model = ModelFactory.createDefaultModel();
			model.setNsPrefix("","http://example.org/");
			testQueryParsing(query, "http://example.org/q" + i, model);
			i++;
		}
	}
	
	public static void testQueryParsing(String query, String handle, Model model) {
		// Initialize SPIN system functions and templates
		SPINModuleRegistry.get().init();

		Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
		Query reparsedQuery = parseSPINReparse(query, handle, model);
		
		PrefixMapping pm = new PrefixMappingImpl();
		pm.setNsPrefixes(model.getNsPrefixMap());
		
		System.out.println("# Original parsed");
		parsedQuery.setPrefixMapping(pm);
		System.out.println(parsedQuery);
		System.out.println("# Reparsed");
		reparsedQuery.setPrefixMapping(pm);
		System.out.println(reparsedQuery);
		
		System.out.println("##############################");		
	}

	/**
	 * Parses a query string to ARQ, converts it to SPIN, and then reads the ARQ
	 * query back again from the model. This should match the original query string.
	 * 
	 * @param queryString
	 * @param handle
	 * @param nsMap
	 * @return arqQuery
	 */
	private static Query parseSPINReparse(String queryString, String handle, Model model) {
		// Parse using ARQ
		Query parsed = QueryFactory.create(queryString, Syntax.syntaxARQ);
		
		// Convert to SPIN
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		arq2SPIN.createQuery(parsed, handle);
		
		// Read SPIN query from the model
		org.topbraid.spin.model.Query query = SPINFactory.asQuery(model.getResource(handle));
		
		// Read the model without prefixes
		StringPrintContext context = new StringPrintContext();
		context.setUsePrefixes(false);
		query.print(context);
		String q = context.getString();
		
		Query reparsed = QueryFactory.create(q, Syntax.syntaxARQ);
		return reparsed;
	}
	
	/**
	 * Read some test queries from queries/test
	 * @return queries
	 */
	private static String[] getTestQueries(){
		try {
			String tests = new String(Files.readAllBytes(Paths.get("./queries/tests.txt")));
			return tests.split(";;;");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new String[]{};
	}
}
