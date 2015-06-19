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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class Namespace extends AbstractResource {

	public static final String ID = "namespaceId";
	public static final String PREFIX = "prefix";
	public static final String URI = "uri";

	private static final Set<String> allowedAttributes = new HashSet<String>();

	static {
		allowedAttributes.add(ID);
		allowedAttributes.add(PREFIX);
		allowedAttributes.add(URI);
	}

	/**
	 * 
	 */
	Namespace(String prefix) {
		setAttribute(PREFIX, prefix);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#name()
	 */
	@Override
	public String name() {
		return (String) getAttribute(PREFIX);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResource#allowedAttributes()
	 */
	@Override
	protected Set<String> allowedAttributes() {
		return Collections.unmodifiableSet(allowedAttributes);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResource#templateLocation()
	 */
	@Override
	protected String templateLocation() {
		return "/template/namespace-template.vm";
	}

}
