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

package eu.esdihumboldt.hale.io.geoserver;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class DataStore extends AbstractResource {

	public static final String ID = "dataStoreId";
	public static final String NAME = "dataStoreName";
	public static final String WORKSPACE_ID = "workspaceId";
	public static final String CONNECTION_PARAMS = "connectionParameters";

	private static final String TEMPLATE_LOCATION = "/eu/esdihumboldt/hale/io/geoserver/template/data/datastore-template.vm";

	private static final Set<String> allowedAttributes = new HashSet<String>();

	static {
		allowedAttributes.add(ID);
		allowedAttributes.add(NAME);
		allowedAttributes.add(WORKSPACE_ID);
		allowedAttributes.add(CONNECTION_PARAMS);
	}

	public DataStore(String name) {
		setAttribute(DataStore.NAME, name);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.Resource#name()
	 */
	@Override
	public String name() {
		return (String) getAttribute(NAME);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.AbstractResource#allowedAttributes()
	 */
	@Override
	protected Set<String> allowedAttributes() {
		return allowedAttributes;
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.AbstractResource#templateLocation()
	 */
	@Override
	protected String templateLocation() {
		return TEMPLATE_LOCATION;
	}

	public Map<String, String> getConnectionParameters() {
		return (Map<String, String>) getAttribute(CONNECTION_PARAMS);
	}
}
