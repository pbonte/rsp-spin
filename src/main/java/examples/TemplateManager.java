package examples;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rspql.lang.rspql.ParserRSPQL;
import org.rspql.spin.utils.TemplateUtils;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.SyntaxError;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateManager {
	private Model model = null;
	private JSONArray templateList = new JSONArray();

	public static void main(String[] args) throws FileNotFoundException {
		new TemplateManager();
	}

	public TemplateManager() {

		// Initialize
		// loadModelSP(); // required for web apps
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		// Eager literal validation active, required
		com.hp.hpl.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;

		// Create model
		model = ModelFactory.createDefaultModel();

		// Create a time value
		Literal time = ResourceFactory.createTypedLiteral("PT10S", XSDDatatype.XSDduration);

		// Load templates into model

		// Template 1
		try {
			String pre = "https://w3id.org/rsp/spin/join";
			Template template = createTemplate(model, "queries/join.rspql", pre);
			if (template != null) {
				template.addLiteral(RDFS.comment, "Join two streams over sliding windows");
				// Add some argument constraints
				TemplateUtils.createArgument(template, "output", RDFS.Resource, null, false,
						"The identifier of the resulting output stream. This should be a valid unoccupied URI.");
				TemplateUtils.createArgument(template, "input1", RDFS.Resource, null, false,
						"The identifier of the first stream. This should be the URI of a registered stream.");
				TemplateUtils.createArgument(template, "input2", RDFS.Resource, null, false,
						"The identifier of the second stream. This should be the URI of a registered stream.");
				TemplateUtils.createArgument(template, "range", XSD.duration, time, false,
						"The range of the windows on which the two streams are joined. This should be valid positive ISO 8601 duration (e.g. PT10S)");
				TemplateUtils.createArgument(template, "o1", XSD.duration, time, true, "test");
			}
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}

		System.out.println(getQuery("https://w3id.org/rsp/spin/join#template", new JSONObject()).get("query"));

	}

	private Template createTemplate(Model model, String file, String handle) throws FileNotFoundException {
		String qString = slurp(file);
		if (qString.equals("")) {
			return null;
		}

		Query query = QueryFactory.create(qString, ParserRSPQL.rspqlSyntax);
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, handle + "#query");
		// Create template based on query
		Template template = TemplateUtils.createTemplate(handle + "#template", spinQuery);
		templateList.put(templateList.length(), handle + "#template");
		return template;
	}

	public String getModel() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		model.write(os, "TTL");
		try {
			return new String(os.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "Error: " + e.getMessage();
		}
	}

	public JSONObject getTemplate(String id) {
		// Get the template from the model using a template resource handle
		Template template = model.createResource(id, SPIN.Template).as(Template.class);

		JSONArray args = new JSONArray();
		List<Argument> list = template.getArguments(true);
		for (Argument arg : list) {
			JSONObject o = new JSONObject();
			o.put("comment", arg.getComment());
			o.put("name", arg.getVarName());
			o.put("optional", arg.isOptional());
			o.put("default", arg.getDefaultValue());
			o.put("type", arg.getValueType());
			args.put(o);
		}

		JSONObject obj = new JSONObject();
		obj.put("comment", template.getProperty(RDFS.comment).getString());
		obj.put("args", args);
		return obj;
	}

	public JSONObject getQuery(String id, JSONObject input) {
		// Result
		JSONObject result = new JSONObject();
		// Get the template from the model using a template resource handle
		Template template = model.createResource(id, SPIN.Template).as(Template.class);
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) template.getBody());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);

		// Bindings
		QuerySolutionMap bindings = new QuerySolutionMap();
		List<Argument> list = template.getArguments(true);
		for (Argument arg : list) {
			String name = arg.getVarName();
			String value = input.has(name) ? input.getString(name) : "";

			if (value.equals("")) {
				// Check for default value in arg
				RDFNode val = arg.getDefaultValue();
				if(val != null) {
					bindings.add(name, val);
				}
				continue;
			}

			try {
				if (arg.getValueType().equals(RDFS.Resource)) {
					URL u = new URL(value);
					u.toURI();
					Resource resource = model.createResource(value);
					bindings.add(name, resource);
				} else {
					RDFDatatype dt = NodeFactory.getType(arg.getValueType().getURI());
					if (dt.equals(XSDDatatype.XSDduration)) {
						value = value.toUpperCase();
					}
					Literal lit = ResourceFactory.createTypedLiteral(value, dt);
					bindings.add(name, lit);
				}
			} catch (Exception e) {
				bindings.add(name, model.createLiteral(value));
			}
		}

		// Validate template with bindings
		TemplateUtils.setOutputStream(ps);
		TemplateUtils.validate(template, bindings);

		arq.setPrefix("xsd", XSD.getURI());
		arq.setPrefix("rdf", RDF.getURI());
		arq.setPrefix("rdfs", RDFS.getURI());
		arq.setPrefix("owl", OWL.getURI());

		// Parameterized, constraint checking in the previous step
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		String qString = TemplateUtils.clean(pss.toString());
		String javaError = "";
		try {
			// Now parse it
			Query query = QueryFactory.create(qString, ParserRSPQL.rspqlSyntax);
			result.put("query", query.toString());
		} catch (Exception e) {
			result.put("query", qString);
			javaError = "Parse error:\n" + e.getMessage();
		}

		result.put("errors", baos.toString());
		result.put("java-error", javaError);
		ps.close();
		return result;
	}

	/**
	 * Read a file into a string.
	 * 
	 * @param path
	 * @return string
	 * @throws FileNotFoundException
	 */
	private String slurp(String path) throws FileNotFoundException {
		InputStream is = new FileInputStream(new File(path));
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			br.close();
			return sb.toString();
		} catch (IOException e) {
			return "";
		}
	}

	public JSONArray getTemplateList() {
		return templateList;
	}

	/**
	 * Manually load the SP model from file
	 */
	public void loadModelSP() {
		URL url = this.getClass().getClassLoader().getResource("etc/sp.ttl");
		Model model = RDFDataMgr.loadModel(url.toString());
		SP.setModel(model);
	}
}
