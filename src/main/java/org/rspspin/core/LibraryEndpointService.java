package org.rspspin.core;

import java.util.ArrayList;

import org.topbraid.spin.model.Template;

/**
 * The LibraryEndpointService interface provides methods for managing templates
 * persisted in a triple store.
 */

public interface LibraryEndpointService {
	public void storeTemplate(Template template) throws RSPSPINException;

	public void setQueryEndpoint(String queryEndpoint);

	public void setUpdateEndpoint(String updateEndpoint);

	public String getUpdateEndpoint();

	public String getQueryEndpoint();

	public void deleteTemplate(String templateUri);

	public Template getTemplate(String templateUri);
	
	public ArrayList<Template> loadTemplates();
}
