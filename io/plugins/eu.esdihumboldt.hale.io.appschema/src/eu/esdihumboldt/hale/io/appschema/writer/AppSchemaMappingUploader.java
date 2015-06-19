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

package eu.esdihumboldt.hale.io.appschema.writer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier;
import eu.esdihumboldt.hale.io.appschema.AppSchemaIO;
import eu.esdihumboldt.hale.io.geoserver.rest.AppSchemaDataStore;
import eu.esdihumboldt.hale.io.geoserver.rest.DataStore;
import eu.esdihumboldt.hale.io.geoserver.rest.DataStoreFile;
import eu.esdihumboldt.hale.io.geoserver.rest.DataStoreFileManager;
import eu.esdihumboldt.hale.io.geoserver.rest.DataStoreManager;
import eu.esdihumboldt.hale.io.geoserver.rest.Namespace;
import eu.esdihumboldt.hale.io.geoserver.rest.NamespaceManager;
import eu.esdihumboldt.hale.io.geoserver.rest.ResourceBuilder;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingUploader extends AbstractAppSchemaConfigurator {

	private URL geoserverURL;
	private String username;
	private String password;

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.AbstractAppSchemaConfigurator#handleMapping(eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingGenerator,
	 *      eu.esdihumboldt.hale.common.core.io.ProgressIndicator,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	protected void handleMapping(AppSchemaMappingGenerator generator, ProgressIndicator progress,
			IOReporter reporter) throws IOProviderConfigurationException, IOException {
		LocatableOutputSupplier<? extends OutputStream> target = getTarget();

		geoserverURL = target.getLocation().toURL();
		username = getParameter(AppSchemaIO.PARAM_USER).as(String.class);
		password = getParameter(AppSchemaIO.PARAM_PASSWORD).as(String.class);

		publishNamespaces(generator);

		publishAppSchemaDataStore(generator);
	}

	private void publishNamespaces(AppSchemaMappingGenerator generator) {
		NamespaceManager nsMgr = new NamespaceManager(geoserverURL);
		nsMgr.setCredentials(username, password);

		// check whether main namespace/workspace exists; if not, create it
		Map<String, String> dataStoreAttributes = generator.getAppSchemaDataStore();
		String mainNsPrefix = dataStoreAttributes.get("prefix");
		String mainNsUri = dataStoreAttributes.get("uri");
		Namespace mainNs = ResourceBuilder.namespace(mainNsPrefix)
				.setAttribute(Namespace.URI, mainNsUri).build();
		nsMgr.setResource(mainNs);
		if (!nsMgr.exists()) {
			nsMgr.create();
		}

		// check whether secondary namespaces/workspaces exist; if not, create
		// them
		Map<String, Map<String, String>> secondaryNamespaces = generator.getSecondaryNamespaces();
		for (String nsPrefix : secondaryNamespaces.keySet()) {
			Map<String, String> nsAttributes = secondaryNamespaces.get(nsPrefix);
			Namespace ns = ResourceBuilder.namespace(nsPrefix)
					.setAttribute(Namespace.URI, nsAttributes.get(Namespace.URI)).build();
			nsMgr.setResource(ns);

			if (!nsMgr.exists()) {
				nsMgr.create();
			}
		}
	}

	private void publishAppSchemaDataStore(AppSchemaMappingGenerator generator) throws IOException {
		Map<String, String> dataStoreAttributes = generator.getAppSchemaDataStore();
		String workspaceName = dataStoreAttributes.get("workspaceName");
		String dataStoreName = dataStoreAttributes.get("dataStoreName");

		// build datastore resource
		DataStore dataStore = ResourceBuilder.dataStore(dataStoreName, AppSchemaDataStore.class)
				.build();
		DataStoreManager dsMgr = new DataStoreManager(geoserverURL);
		dsMgr.setCredentials(username, password);
		dsMgr.setResource(dataStore);
		dsMgr.setWorkspace(workspaceName);
		// remove datastore, if necessary
		if (dsMgr.exists()) {
			Map<String, String> deleteParams = new HashMap<String, String>();
			deleteParams.put("recurse", "true");

			dsMgr.delete(deleteParams);
		}

		// build mapping file resource
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		generator.writeMappingConf(os);
		DataStoreFile mappingFile = ResourceBuilder
				.dataStoreFile(new ByteArrayInputStream(os.toByteArray()))
				.setAttribute(DataStoreFile.EXTENSION, "appschema")
				.setAttribute(DataStoreFile.DATASTORE, dataStoreName)
				.setAttribute(DataStoreFile.WORKSPACE, workspaceName).build();
		DataStoreFileManager dsFileMgr = new DataStoreFileManager(geoserverURL);
		dsFileMgr.setCredentials(username, password);
		dsFileMgr.setResource(mappingFile);

		Map<String, String> updateParams = new HashMap<String, String>();
		updateParams.put("configure", "all");
		dsFileMgr.update(updateParams);
	}
}
