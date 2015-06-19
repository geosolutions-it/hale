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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import com.google.common.base.Joiner;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class DataStoreManager extends AbstractResourceManager<DataStore> {

	private String workspace;

	/**
	 * @param geoserverUrl
	 * @throws MalformedURLException
	 */
	public DataStoreManager(String geoserverUrl) throws MalformedURLException {
		super(geoserverUrl);
	}

	public DataStoreManager(URL geoserverUrl) {
		super(geoserverUrl);
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResourceManager#getResourceListPath()
	 */
	@Override
	protected String getResourceListPath() {
		checkWorkspaceSet();

		return Joiner.on('/').join(Arrays.asList("workspaces", workspace, "datastores"));
	}

	/**
	 * @see eu.esdihumboldt.hale.io.geoserver.rest.AbstractResourceManager#getResourcePath()
	 */
	@Override
	protected String getResourcePath() {
		checkWorkspaceSet();

		return Joiner.on('/').join(Arrays.asList(getResourceListPath(), resource.name()));
	}

	private void checkWorkspaceSet() {
		if (workspace == null || workspace.isEmpty()) {
			throw new IllegalStateException("Workspace not set");
		}
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

}
