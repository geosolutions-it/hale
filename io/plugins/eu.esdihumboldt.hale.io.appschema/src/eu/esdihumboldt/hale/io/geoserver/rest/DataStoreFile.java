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

package eu.esdihumboldt.hale.io.geoserver.rest;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class DataStoreFile extends AbstractResource {

	public static final String WORKSPACE = "ws";
	public static final String DATASTORE = "ds";
	public static final String EXTENSION = "extension";

	private static final Set<String> allowedAttributes = new HashSet<String>();

	static {
		allowedAttributes.add(WORKSPACE);
		allowedAttributes.add(DATASTORE);
		allowedAttributes.add(EXTENSION);
	}

	private final InputStream resourceStream;

	public DataStoreFile(InputStream resourceStream) {
		this.resourceStream = resourceStream;
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#name()
	 */
	@Override
	public String name() {
		return resourceStream.toString();
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResource#resourceStream()
	 */
	@Override
	protected InputStream resourceStream() {
		return resourceStream;
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResource#allowedAttributes()
	 */
	@Override
	protected Set<String> allowedAttributes() {
		return allowedAttributes;
	}

	public static enum Extension {

		shp, properties, h2, spatialite, appschema

	}
}
