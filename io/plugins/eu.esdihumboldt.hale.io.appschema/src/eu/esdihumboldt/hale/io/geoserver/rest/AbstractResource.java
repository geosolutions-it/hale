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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.entity.ContentType;

import eu.esdihumboldt.hale.io.appschema.Templates;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class AbstractResource implements Resource {

	public static final ContentType DEF_CONTENT_TYPE = ContentType.APPLICATION_XML
			.withCharset("UTF-8");

	protected Map<String, Object> attributes;
	protected Set<String> allowedAttributes;

	/**
	 * 
	 */
	public AbstractResource() {
		this.attributes = new HashMap<String, Object>();
		this.allowedAttributes = new HashSet<String>();
		this.allowedAttributes.addAll(allowedAttributes());
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#contentType()
	 */
	@Override
	public ContentType contentType() {
		return DEF_CONTENT_TYPE;
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name must be set");
		}

		return this.attributes.get(name);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#setAttribute(java.lang.String,
	 *      java.lang.Object)
	 */
	@Override
	public void setAttribute(String name, String value) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name must be set");
		}
		if (!this.allowedAttributes.contains(name)) {
			throw new IllegalArgumentException(String.format(
					"Variable \"%s\" not allowed in this resource", name));
		}

		this.attributes.put(name, value);
	}

	/**
	 * @throws IOException
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#print(java.io.OutputStream)
	 */
	@Override
	public void print(OutputStream out) throws IOException {

		// unset unspecified variables by setting their value to null
		for (String var : this.allowedAttributes) {
			if (!this.attributes.containsKey(var)) {
				this.attributes.put(var, null);
			}
		}

		InputStream resourceStream = loadResource();
		if (resourceStream != null) {
			BufferedInputStream input = new BufferedInputStream(resourceStream);
			BufferedOutputStream output = new BufferedOutputStream(out);
			try {

				for (int b = input.read(); b > 0; b = input.read()) {
					output.write(b);
				}
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// ignore exception on close
				}
				try {
					output.close();
				} catch (IOException e) {
					// ignore exception on close
				}
			}
		}

	}

	protected InputStream loadResource() throws IOException {

		if (templateLocation() != null && !templateLocation().isEmpty()) {
			// resource is loaded from a template and attributes map is used to
			// interpolate variables in it
			return Templates.getInstance().loadTemplate(templateLocation(), this.attributes);
		}
		else if (resourceStream() != null) {
			// load resource straight from the classpath
			return resourceStream();
		}

		return null;

	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#asStream()
	 */
	@Override
	public InputStream asStream() throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(asByteArray());

		return bis;
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.Resource#asByteArray()
	 */
	@Override
	public byte[] asByteArray() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			print(bos);

			return bos.toByteArray();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				// ignore exception on close
			}
		}
	}

	/**
	 * Should be overridden by sublcasses loading resource content from
	 * template.
	 * 
	 * @return
	 */
	protected String templateLocation() {
		return null;
	}

	/**
	 * Should be overridden by subclasses loading resource content from an
	 * {@link InputStream}, e.g. file resources.
	 * 
	 * @return
	 */
	protected InputStream resourceStream() {
		return null;
	}

	protected abstract Set<String> allowedAttributes();
}
