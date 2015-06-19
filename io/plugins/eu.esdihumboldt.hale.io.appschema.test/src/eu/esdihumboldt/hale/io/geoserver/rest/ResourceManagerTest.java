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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.esdihumboldt.hale.io.geoserver.rest.DataStoreFile.Extension;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class ResourceManagerTest {

	// TODO: make this configurable
	public static final String GEOSERVER_URL = "http://localhost:8080/geoserver";
	public static final String GEOSERVER_USER = "admin";
	public static final String GEOSERVER_PASSWORD = "geoserver";

	public static final String NAMESPACE_PREFIX = "hale";
	public static final String NAMESPACE_URI = "http://www.esdi-community.eu/projects/hale";
	public static final String NAMESPACE_URI_ALT = "http://www.esdi-community.eu/projects/hale_alt";

	public static final String APP_SCHEMA_MAPPING_FILE = "/data/LandCoverVector.xml";
	public static final String APP_SCHEMA_DATASTORE = "LandCoverVector";
	public static final String APP_SCHEMA_WORKSPACE = "lcv";
	public static final String APP_SCHEMA_URI = "http://inspire.ec.europa.eu/schemas/lcv/3.0";

	@Test
	@Ignore
	public void testNamespaceManager() throws Exception {

		Namespace ns = ResourceBuilder.namespace(NAMESPACE_PREFIX)
				.setAttribute(Namespace.URI, NAMESPACE_URI).build();
		NamespaceManager nsMgr = new NamespaceManager(GEOSERVER_URL);

		nsMgr.setCredentials(GEOSERVER_USER, GEOSERVER_PASSWORD);
		nsMgr.setResource(ns);

		assertFalse(nsMgr.exists());

		// create namespace
		URL nsURL = nsMgr.create();
		assertNotNull(nsURL);

		assertTrue(nsMgr.exists());

		// check namespace was created correctly
		Document nsDoc = nsMgr.read();
		assertEquals("namespace", nsDoc.getDocumentElement().getNodeName());

		NodeList prefixNodes = nsDoc.getElementsByTagName("prefix");
		assertEquals(1, prefixNodes.getLength());
		assertEquals(NAMESPACE_PREFIX, prefixNodes.item(0).getTextContent());

		NodeList uriNodes = nsDoc.getElementsByTagName("uri");
		assertEquals(1, uriNodes.getLength());
		assertEquals(NAMESPACE_URI, uriNodes.item(0).getTextContent());

		// update namespace URI
		ns.setAttribute(Namespace.URI, NAMESPACE_URI_ALT);
		nsMgr.update();

		nsDoc = nsMgr.read();
		uriNodes = nsDoc.getElementsByTagName("uri");
		assertEquals(1, uriNodes.getLength());
		assertEquals(NAMESPACE_URI_ALT, uriNodes.item(0).getTextContent());

		// delete namespace
		nsMgr.delete();
		assertFalse(nsMgr.exists());
	}

	@Test
	@Ignore
	public void testDataStoreManager() throws Exception {

		DataStore ds = ResourceBuilder.dataStore(APP_SCHEMA_DATASTORE, AppSchemaDataStore.class)
				.build();
		DataStoreManager dsMgr = new DataStoreManager(GEOSERVER_URL);

		dsMgr.setCredentials(GEOSERVER_USER, GEOSERVER_PASSWORD);
		dsMgr.setWorkspace(APP_SCHEMA_WORKSPACE);

		Document listDoc = dsMgr.list();
		assertEquals("dataStores", listDoc.getDocumentElement().getNodeName());

		NodeList dataStoreNodes = listDoc.getElementsByTagName("dataStore");
		assertEquals(1, dataStoreNodes.getLength());

		NodeList dataStoreChildren = dataStoreNodes.item(0).getChildNodes();
		for (int i = 0; i < dataStoreChildren.getLength(); i++) {
			Node child = dataStoreChildren.item(i);
			if ("name".equals(child.getNodeName())) {
				assertEquals(APP_SCHEMA_DATASTORE, child.getTextContent());
				break;
			}
		}

		dsMgr.setResource(ds);

		Map<String, String> deleteParams = new HashMap<String, String>();
		deleteParams.put("recurse", "true");
		dsMgr.delete(deleteParams);

		assertFalse(dsMgr.exists());

	}

	@Test
//	@Ignore
	public void testDataStoreFileManager() throws Exception {

		Namespace ns = ResourceBuilder.namespace(APP_SCHEMA_WORKSPACE)
				.setAttribute(Namespace.URI, APP_SCHEMA_URI).build();
		NamespaceManager nsMgr = new NamespaceManager(GEOSERVER_URL);

		nsMgr.setCredentials(GEOSERVER_USER, GEOSERVER_PASSWORD);
		nsMgr.setResource(ns);

		assertTrue(nsMgr.exists());

		// create namespace
//		URL nsURL = nsMgr.create();
//		assertNotNull(nsURL);
//		assertTrue(nsMgr.exists());

		InputStream resourceStream = getClass().getResourceAsStream(APP_SCHEMA_MAPPING_FILE);
		assertNotNull(resourceStream);
		DataStoreFile mappingFile = ResourceBuilder.dataStoreFile(resourceStream)
				.setAttribute(DataStoreFile.WORKSPACE, APP_SCHEMA_WORKSPACE)
				.setAttribute(DataStoreFile.DATASTORE, APP_SCHEMA_DATASTORE)
				.setAttribute(DataStoreFile.EXTENSION, Extension.appschema.name()).build();
		DataStoreFileManager dsFileMgr = new DataStoreFileManager(GEOSERVER_URL);
		dsFileMgr.setCredentials(GEOSERVER_USER, GEOSERVER_PASSWORD);
		dsFileMgr.setResource(mappingFile);

		// upload mapping file (datastore is created implicitly)
		Map<String, String> updateParameters = new HashMap<String, String>();
		updateParameters.put("configure", "all");
		dsFileMgr.update(updateParameters);

		assertTrue(dsFileMgr.exists());
	}
}
