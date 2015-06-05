/*
 * Copyright (c) 2015 Data Harmonisation Panel
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.io.appschema;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class AppSchemaIO {

	public static final String APP_SCHEMA_NAMESPACE = "http://www.geotools.org/app-schema";
	public static final String APP_SCHEMA_PREFIX = "as";
	public static final String CONTENT_TYPE_MAPPING = "eu.esdihumboldt.hale.io.appschema.mapping";
	public static final String CONTENT_TYPE_ARCHIVE = "eu.esdihumboldt.hale.io.appschema.archive";

	public static final String MAPPING_TEMPLATE = "/template/mapping-template.xml";
	public static final String NAMESPACE_TEMPLATE = "/template/namespace-template.xml";
	public static final String WORKSPACE_TEMPLATE = "/template/workspace-template.xml";
	public static final String DATASTORE_TEMPLATE = "/template/datastore-template.xml";
	public static final String FEATURETYPE_TEMPLATE = "/template/featuretype-template.xml";
	public static final String LAYER_TEMPLATE = "/template/layer-template.xml";

	public static final String NAMESPACE_FILE = "namespace.xml";
	public static final String WORKSPACE_FILE = "workspace.xml";
	public static final String DATASTORE_FILE = "datastore.xml";
	public static final String FEATURETYPE_FILE = "featuretype.xml";
	public static final String LAYER_FILE = "layer.xml";

	public static InputStream loadTemplate(String templateResource, Map<String, String> variables)
			throws IOException {
		InputStream templateInput = AppSchemaIO.class.getResourceAsStream(templateResource);
		if (templateInput == null) {
			return null;
		}
		if (variables == null || variables.isEmpty()) {
			return templateInput;
		}

		// read template content
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(templateInput));
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			// add platform-independent newline char (works on JDK 5+)
			builder.append(String.format("%n"));
		}
		String templateContent = builder.toString();
		// interpolate variables
		for (Entry<String, String> variable : variables.entrySet()) {
			String varName = variable.getKey();
			String varValue = variable.getValue();
			if (varValue == null) {
				varValue = "";
			}
			templateContent = templateContent.replace("${" + varName + "}", varValue);
		}

		// return template content as InputStream
		return new ByteArrayInputStream(templateContent.getBytes(Charset.forName("UTF-8")));
	}

}
