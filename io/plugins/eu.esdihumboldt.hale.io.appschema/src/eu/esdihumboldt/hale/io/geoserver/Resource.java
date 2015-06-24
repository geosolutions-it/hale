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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.ContentType;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public interface Resource {

	public String name();

	public ContentType contentType();

	public Object getAttribute(String name);

	public void setAttribute(String name, Object value);

	public void print(OutputStream out) throws IOException;

	public InputStream asStream() throws IOException;

	public byte[] asByteArray() throws IOException;

}
