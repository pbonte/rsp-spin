package org.rspspin.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.XSD;
import org.apache.log4j.Logger;
import org.json.JSONArray;

public class RSPSPINUtils {
	private static Logger logger = Logger.getLogger(RSPSPINUtils.class);
	private static PrefixMapping prefixes = null;
	private static String prefixPath = "rspql.prefixes";
	private static String propsPath = "rspql.properties";
	private static Properties props = null;

	static {
		loadProperties();
	}

	/**
	 * Set the path to the properties file
	 * 
	 * @param path
	 */
	public static void setPropertiesPath(String path) {
		propsPath = path;
	}

	/**
	 * Load properties
	 * 
	 * @param path
	 */
	public static void loadProperties() {
		try {
			props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propsPath));
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Check the validity of a URI (scheme required)
	 * 
	 * @param uri
	 * @return
	 */
	public static boolean isValidUri(String uri) {
		try {
			if (new URI(uri).getScheme() != null)
				return true;
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	/**
	 * Check the validity of the fragment part of template id.
	 * 
	 * @param id
	 * @return
	 */
	public static boolean isValidTemplateId(String id) {
		return !id.matches("[^A-Za-z0-9\\-_\\(\\)]");
	}

	/**
	 * Validate SPARQL variable name
	 * 
	 * @param varName
	 * @return
	 */
	public static boolean isValidVarName(String varName) {
		return varName.matches("^[A-Za-z_0-9]+$");
	}

	/**
	 * Convenience method to create a new resource from a string that
	 * potentially represents an XSD data type.
	 * 
	 * @param type
	 * @return
	 * @throws BadRequestException
	 */
	public static Resource createResource(String value) {
		value = prefixes.expandPrefix(value);

		// If XSD data type
		if (value.startsWith(XSD.getURI())) {
			XSDDatatype datatype = new XSDDatatype(value.split("#")[1]);
			return ResourceFactory.createResource(datatype.getURI());
		}
		return ResourceFactory.createResource(value);
	}

	public static RDFNode createRDFNode(String value, String valueType) throws RSPSPINException {
		value = prefixes.expandPrefix(value);
		if (valueType != null)
			valueType = prefixes.expandPrefix(valueType);

		// No value type
		if (valueType == null) {
			if (isValidUri(value))
				return ResourceFactory.createResource(value);
			return ResourceFactory.createTypedLiteral(value);
		}

		// XSD data type
		if (valueType.startsWith(XSD.getURI())) {
			XSDDatatype type = new XSDDatatype(valueType.split("#")[1]);
			Literal lit = ResourceFactory.createTypedLiteral(value, type);
			// Validation is only done when reading the literal
			try {
				lit.getValue();
			} catch (DatatypeFormatException e) {
				throw new RSPSPINException(
						String.format("Value '%s' could not be parsed as instance of datatype '%s'", value, valueType));
			}
			return lit;
		}
		if (!isValidUri(value))
			throw new RSPSPINException(String.format("'%s' is not a valid resource URI", value));
		return ResourceFactory.createResource(value);
	}

	/**
	 * Initialize default prefixes
	 */
	public static void initPrefixes() {
		try {
			Properties props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(prefixPath));
			prefixes = ModelFactory.createDefaultModel();
			props.keySet().forEach((key) -> {
				prefixes.setNsPrefix(key.toString(), props.get(key).toString());
			});
			System.out.printf("Prefixes loaded from %s:\n%s\n", prefixPath, prefixes.getNsPrefixMap());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Get default prefixes
	 * 
	 * @return
	 */
	public static PrefixMapping getDefaultPrefixMapping() {
		if (prefixes == null)
			initPrefixes();
		return prefixes;
	}

	/**
	 * Override default prefixes
	 * 
	 * @param prefixMapping
	 */
	public static void setDefaultPrefixMapping(PrefixMapping prefixMapping) {
		RSPSPINUtils.prefixes = prefixMapping;
	}

	/**
	 * Create and empty model with default prefixes
	 */
	public static Model createDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(getDefaultPrefixMapping());
		return model;
	}

	/**
	 * Convert a JSON array to a bag of typed values.
	 * 
	 * @param values
	 * @throws RSPSPINException
	 */
	public static Bag asBag(JSONArray values, RDFNode valueType) throws RSPSPINException {
		Model model = ModelFactory.createDefaultModel();
		Bag bag = model.createBag();
		for (Object value : values) {
			String type = valueType != null ? valueType.toString() : null;
			bag.add(RSPSPINUtils.createRDFNode(value.toString(), type));
		}
		return bag;
	}
}
