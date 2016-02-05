package org.rspql.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.rspql.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SP;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RSPQL {
	static RSPQL api = new RSPQL();

	/**
	 * Do not create instances of the API, instead get the singleton using
	 * API.get()
	 */
	public RSPQL() {
		// Initialize SPIN system functions and templates
		SPINModuleRegistry.get().init();
		// Register the RSP-QL parser
		ParserRSPQL.register();
		// Set RSP-QL as the ARQ factory singleton syntax
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
	}

	/**
	 * Get a reference to the API singleton.
	 * 
	 * @return
	 */
	public static RSPQL get() {
		return api;
	}

	/**
	 * Parse an RSP-QL query and return a query object.
	 * 
	 * @param queryString
	 * @return query
	 */
	public Query parseQuery(String queryString) {
		Query query = QueryFactory.create(queryString, ParserRSPQL.rspqlSyntax);
		return query;
	}

	/**
	 * Serialize a query object as RDF.
	 * 
	 * @param query
	 * @return
	 */
	public String queryToRDF(Query query, String handle) {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("sp", SP.NS);
		model.setNsPrefix("rsp", SP.RSP);
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		arq2SPIN.createQuery(query, handle);

		model.write(System.out, "TTL");
		return "";
	}

	/**
	 * Serialize a query object as RDF.
	 * 
	 * @param query
	 * @return
	 * @throws IOException
	 */
	public Query queryFromRDF(String input, String handle) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		model.read(new ByteArrayInputStream((input).getBytes()), null, "TTL");
		model.setNsPrefix("sp", SP.NS);
		model.setNsPrefix("rsp", SP.RSP);
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

		org.topbraid.spin.model.Query spinQuery = SPINFactory.asQuery(model.getResource(handle));
		Query query = ARQFactory.get().createQuery(spinQuery);
		return query;
	}

	/**
	 * Basic command line interaction.
	 * 
	 * @param args
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {

		String in = null;
		String handle = null;
		String mode = null;
		int i = 0;
		while (args.length > i) {
			if (args[i].equals("--help")) {
				printHelp();
				System.exit(0);
			} else if (args[i].equals("-f") || args[i].equals("--file")) {
				i++;
				in = new String(Files.readAllBytes(Paths.get(args[i])));
			} else if (args[i].equals("-i") || args[i].equals("--input")) {
				i++;
				if (in == null) {
					in = args[i];
				}
			} else if (args[i].equals("-h") || args[i].equals("--handle")) {
				i++;
				handle = args[i];
			} else if (args[i].equals("-m") || args[i].equals("--mode")) {
				i++;
				mode = args[i];
			}
			i++;
		}

		// Check that 'mode' is specified
		if (mode == null || (!mode.equals("query") && !mode.equals("rdf"))) {
			System.err.println("Please specify mode as either 'query' or 'rdf'");
			System.exit(1);
		}

		// Check that 'in' is specified
		if (in == null) {
			System.err.println("Please specify input or input file");
			System.exit(1);
		}

		// Check that 'handle' is specified
		if (mode.equals("rdf") && handle == null) {
			System.err.println("Please specify a URI handle to parse a query from RDF");
			System.exit(1);
		}

		// Query to RDF
		if (mode.equals("query")) {
			RSPQL api = RSPQL.get();
			Query parsedQuery = QueryFactory.create(in, ParserRSPQL.rspqlSyntax);
			api.queryToRDF(parsedQuery, handle); // should write stream to string
		} else {
			// Get query from RDF
			RSPQL api = RSPQL.get();
			Query query = api.queryFromRDF(in, handle);
			System.out.println(query);
		}
	}

	public static void printHelp() {
		System.out.println("\nUsage\n" + "   --help       Display help\n" + "-f --file    Path to input file\n"
				+ "   -i --input   Input string. If a file is specified this argument is ignored\n"
				+ "   -h --handle  Specify the URI handle (optional if parsing a query)\n"
				+ "   -m --mode    Parse from 'query' or 'rdf'\n");
	}
}
