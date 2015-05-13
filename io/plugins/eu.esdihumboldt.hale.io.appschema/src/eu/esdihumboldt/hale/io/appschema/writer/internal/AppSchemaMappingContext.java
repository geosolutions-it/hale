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

package eu.esdihumboldt.hale.io.appschema.writer.internal;

import java.util.Map;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.IncludesPropertyType;
import org.geotools.app_schema.NamespacesPropertyType;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.SourceDataStoresPropertyType;
import org.geotools.app_schema.TargetTypesPropertyType;
import org.geotools.app_schema.TargetTypesPropertyType.FeatureType;
import org.geotools.app_schema.TypeMappingsPropertyType;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingContext {

	private final String defaultPrefix = "nns";
	private int prefixCounter = 1;
	private Map<String, Namespace> namespaceUriMap;
	private Map<String, Namespace> namespacePrefixMap;

	private final AppSchemaDataAccessType appSchemaMapping;

	/**
	 * 
	 */
	public AppSchemaMappingContext(AppSchemaDataAccessType appSchemaMapping) {
		this.appSchemaMapping = appSchemaMapping;

		if (this.appSchemaMapping.getNamespaces() == null) {
			this.appSchemaMapping.setNamespaces(new NamespacesPropertyType());
		}
		if (this.appSchemaMapping.getSourceDataStores() == null) {
			this.appSchemaMapping.setSourceDataStores(new SourceDataStoresPropertyType());
		}
		if (this.appSchemaMapping.getIncludedTypes() == null) {
			this.appSchemaMapping.setIncludedTypes(new IncludesPropertyType());
		}
		if (this.appSchemaMapping.getTargetTypes() == null) {
			this.appSchemaMapping.setTargetTypes(new TargetTypesPropertyType());
			this.appSchemaMapping.getTargetTypes().setFeatureType(new FeatureType());
		}
		if (this.appSchemaMapping.getTypeMappings() == null) {
			this.appSchemaMapping.setTypeMappings(new TypeMappingsPropertyType());
		}
	}

	public Namespace getOrCreateNamespace(String namespaceURI, String prefix) {
		if (namespaceURI != null && !namespaceURI.isEmpty()
				&& !namespaceUriMap.containsKey(namespaceURI)) {
			if (prefix == null || prefix.trim().isEmpty()) {
				prefix = defaultPrefix;
			}
			// make sure prefix is unique
			String uniquePrefix = prefix;
			while (namespacePrefixMap.containsKey(uniquePrefix)) {
				uniquePrefix = prefix + prefixCounter;
				prefixCounter++;
			}

			Namespace ns = new Namespace();
			ns.setPrefix(uniquePrefix);
			ns.setUri(namespaceURI);

			namespaceUriMap.put(namespaceURI, ns);
			namespacePrefixMap.put(uniquePrefix, ns);

			appSchemaMapping.getNamespaces().getNamespace().add(ns);

			return ns;
		}
		else {
			return namespaceUriMap.get(namespaceURI);
		}
	}

	public void addSchemaURI(String schemaURI) {
		if (schemaURI != null && !schemaURI.isEmpty()) {
			this.appSchemaMapping.getTargetTypes().getFeatureType().getSchemaUri().add(schemaURI);
		}
	}
}
