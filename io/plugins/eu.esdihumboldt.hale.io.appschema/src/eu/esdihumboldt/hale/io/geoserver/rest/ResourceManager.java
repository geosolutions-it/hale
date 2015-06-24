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

import java.net.URL;
import java.util.Map;

import org.w3c.dom.Document;

import eu.esdihumboldt.hale.io.geoserver.Resource;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public interface ResourceManager<T extends Resource> {

	public void setCredentials(String user, String password);

	public Document list();

	public void setResource(T resource);

	public boolean exists();

	public Document read();

	public Document read(Map<String, String> parameters);

	public URL create();

	public URL create(Map<String, String> parameters);

	public void update();

	public void update(Map<String, String> parameters);

	public void delete();

	public void delete(Map<String, String> parameters);

}
