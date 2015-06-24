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

	public static final String PARAM_DATASTORE = "appschema.source.datastore";
	public static final String PARAM_USER = "appschema.rest.user";
	public static final String PARAM_PASSWORD = "appschema.rest.password";

	public static final String MAPPING_TEMPLATE = "/eu/esdihumboldt/hale/io/geoserver/template/data/mapping-template.xml";
	public static final String NAMESPACE_FILE = "namespace.xml";
	public static final String WORKSPACE_FILE = "workspace.xml";
	public static final String DATASTORE_FILE = "datastore.xml";
	public static final String FEATURETYPE_FILE = "featuretype.xml";
	public static final String LAYER_FILE = "layer.xml";

}
