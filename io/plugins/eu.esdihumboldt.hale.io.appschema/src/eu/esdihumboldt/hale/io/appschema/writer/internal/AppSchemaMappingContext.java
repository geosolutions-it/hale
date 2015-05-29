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

import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.findOwningFeatureType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.IncludesPropertyType;
import org.geotools.app_schema.NamespacesPropertyType;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.SourceDataStoresPropertyType;
import org.geotools.app_schema.TargetTypesPropertyType;
import org.geotools.app_schema.TargetTypesPropertyType.FeatureType;
import org.geotools.app_schema.TypeMappingsPropertyType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings;

import com.google.common.base.Joiner;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingContext {

	public static final String FEATURE_LINK_FIELD = "FEATURE_LINK";

	private final String defaultPrefix = "nns";
	private int prefixCounter = 1;
	private final Map<String, Namespace> namespaceUriMap;
	private final Map<String, Namespace> namespacePrefixMap;
	private final Map<Integer, FeatureTypeMapping> featureTypeMappings;

	private final Alignment alignment;
	private final AppSchemaDataAccessType appSchemaMapping;

	/**
	 * 
	 */
	public AppSchemaMappingContext(Alignment alignment, AppSchemaDataAccessType appSchemaMapping) {
		this.alignment = alignment;
		this.appSchemaMapping = appSchemaMapping;

		this.namespaceUriMap = new HashMap<String, Namespace>();
		this.namespacePrefixMap = new HashMap<String, Namespace>();
		this.featureTypeMappings = new HashMap<Integer, FeatureTypeMapping>();

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
		}
		if (this.appSchemaMapping.getTargetTypes().getFeatureType() == null) {
			this.appSchemaMapping.getTargetTypes().setFeatureType(new FeatureType());
		}
		if (this.appSchemaMapping.getTypeMappings() == null) {
			this.appSchemaMapping.setTypeMappings(new TypeMappingsPropertyType());
		}
	}

	public Namespace getOrCreateNamespace(String namespaceURI, String prefix) {
		if (namespaceURI != null && !namespaceURI.isEmpty()) {
			if (!namespaceUriMap.containsKey(namespaceURI)) {
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
				// update prefix if provided prefix is not empty and currently
				// assigned prefix was made up
				Namespace ns = namespaceUriMap.get(namespaceURI);
				if (prefix != null && !prefix.isEmpty() && ns.getPrefix().startsWith(defaultPrefix)) {
					// // check prefix is unique
					// if (!namespacePrefixMap.containsKey(prefix)) {
					// remove old prefix-NS mapping from namespacePrefixMap
					namespacePrefixMap.remove(ns.getPrefix());
					// add new prefix-NS mapping to namespacePrefixMap
					ns.setPrefix(prefix);
					namespacePrefixMap.put(prefix, ns);
					// }
				}
				return ns;
			}
		}
		else {
			return null;
		}
	}

	public void addSchemaURI(String schemaURI) {
		if (schemaURI != null && !schemaURI.isEmpty()) {
			this.appSchemaMapping.getTargetTypes().getFeatureType().getSchemaUri().add(schemaURI);
		}
	}

	public String buildAttributeXPath(PropertyEntityDefinition propertyEntityDef) {
		List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();

		return buildAttributeXPath(propertyPath);
	}

	public String buildAttributeXPath(List<ChildContext> propertyPath) {

		List<String> pathSegments = new ArrayList<String>();
		TypeDefinition owningFeatureType = findOwningFeatureType(propertyPath);
		for (int i = propertyPath.size() - 1; i >= 0; i--) {
			ChildContext childContext = propertyPath.get(i);
			// TODO: how to handle conditions?
			Integer contextId = childContext.getContextName();
			PropertyDefinition child = childContext.getChild().asProperty();
			if (child != null) {
				String namespaceURI = child.getName().getNamespaceURI();
				String prefix = child.getName().getPrefix();
				String name = child.getName().getLocalPart();

				Namespace ns = getOrCreateNamespace(namespaceURI, prefix);
				String path = ns.getPrefix() + ":" + name;
				if (contextId != null) {
					// XPath indices start from 1, whereas contextId starts from
					// 0
					// --> add 1
					path = String.format("%s[%d]", path, contextId + 1);
				}
				// insert path segment at the first position
				pathSegments.add(0, path);

				if (child.getParentType().getName().equals(owningFeatureType.getName())) {
					// I reached a nested feature type: stop walking the path
					// TODO: for sure this will not work always and everywhere
					break;
				}
			}
		}

		String xPath = Joiner.on("/").join(pathSegments);

		return xPath;

	}

	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType) {
		return getOrCreateFeatureTypeMapping(targetType, null);
	}

	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType,
			String mappingName) {
		if (targetType == null) {
			return null;
		}

		Integer hashKey = getFeatureTypeMappingHashKey(targetType, mappingName);
		if (!featureTypeMappings.containsKey(hashKey)) {
			// create
			FeatureTypeMapping featureTypeMapping = new FeatureTypeMapping();
			// initialize attribute mappings member
			featureTypeMapping.setAttributeMappings(new AttributeMappings());
			// TODO: I'm getting the element name with
			// targetType.getDisplayName():
			// isn't there a more elegant (and perhaps more reliable) way to
			// know which element corresponds to a type?
			featureTypeMapping.setTargetElement(targetType.getName().getPrefix() + ":"
					+ targetType.getDisplayName());

			appSchemaMapping.getTypeMappings().getFeatureTypeMapping().add(featureTypeMapping);
			featureTypeMappings.put(hashKey, featureTypeMapping);
		}
		return featureTypeMappings.get(hashKey);
	}

	private Integer getFeatureTypeMappingHashKey(TypeDefinition targetType, String mappingName) {
		int hashKey = targetType.getName().hashCode();
		if (mappingName != null) {
			hashKey &= mappingName.hashCode();
		}

		return hashKey;
	}

	/**
	 * @return the alignment
	 */
	public Alignment getAlignment() {
		return alignment;
	}

	/**
	 * @return the appSchemaMapping
	 */
	public AppSchemaDataAccessType getAppSchemaMapping() {
		return appSchemaMapping;
	}

}
