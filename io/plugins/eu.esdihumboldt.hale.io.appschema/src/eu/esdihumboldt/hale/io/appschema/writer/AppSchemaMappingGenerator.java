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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.ObjectFactory;
import org.geotools.app_schema.SourceDataStoresPropertyType.DataStore;
import org.geotools.app_schema.SourceDataStoresPropertyType.DataStore.Parameters.Parameter;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.io.appschema.AppSchemaIO;
import eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingWrapper;
import eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler;
import eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandlerFactory;
import eu.esdihumboldt.hale.io.appschema.writer.internal.TypeTransformationHandler;
import eu.esdihumboldt.hale.io.appschema.writer.internal.TypeTransformationHandlerFactory;
import eu.esdihumboldt.hale.io.appschema.writer.internal.UnsupportedTransformationException;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingGenerator {

	private static final ALogger log = ALoggerFactory.getLogger(AppSchemaMappingGenerator.class);

	private final Alignment alignment;
	private final SchemaSpace targetSchemaSpace;
	private final Schema targetSchema;
	private final DataStore dataStore;
	private AppSchemaMappingWrapper mappingWrapper;

	public AppSchemaMappingGenerator(Alignment alignment, SchemaSpace targetSchemaSpace,
			DataStore dataStore) {
		this.alignment = alignment;
		this.targetSchemaSpace = targetSchemaSpace;
		// pick the target schemas from which interpolation variables will be
		// derived
		this.targetSchema = pickTargetSchema();
		this.dataStore = dataStore;
	}

	public Schema getTargetSchema() {
		return targetSchema;
	}

	public AppSchemaMappingWrapper generateMapping(IOReporter reporter) throws IOException {
		AppSchemaDataAccessType mapping = loadMappingTemplate();
		// reset wrapper
		mappingWrapper = new AppSchemaMappingWrapper(mapping);

		// create namespace objects for all target types / properties
		// TODO: this removes all namespaces that were defined in the
		// template file, remove it in production
		mapping.getNamespaces().getNamespace().clear();
		createNamespaces();

		// apply datastore configuration, if any
		// TODO: for now, only a single datastore is supported
		applyDataStoreConfig();

		// populate targetTypes element
		createTargetTypes();

		// populate typeMappings element
		createTypeMappings(reporter);

		return mappingWrapper;
	}

	public void generateMapping(OutputStream output, IOReporter reporter) throws IOException {
		generateMapping(reporter);

		writeMappingConf(output);
	}

	public Map<String, String> getAppSchemaDataStore() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		Map<String, String> variables = new HashMap<String, String>();

		Namespace ns = mappingWrapper.getOrCreateNamespace(targetSchema.getNamespace(), null);
		variables.putAll(getWorkspaceInterpolationVariables(ns));

		String dataStoreName = extractSchemaName(targetSchema.getLocation());
		String dataStoreId = dataStoreName + "_datastore";
		String mappingFileName = dataStoreName + ".xml";

		variables.put("dataStoreId", dataStoreId);
		variables.put("dataStoreName", dataStoreName);
		variables.put("mappingFileName", mappingFileName);

		return variables;
	}

	public Map<String, Map<String, String>> getSecondaryNamespaces() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		Map<String, Map<String, String>> secondaryNamespaces = new HashMap<String, Map<String, String>>();
		for (Namespace ns : mappingWrapper.getAppSchemaMapping().getNamespaces().getNamespace()) {
			if (!ns.getUri().equals(getTargetSchema().getNamespace())) {
				Map<String, String> variables = getWorkspaceInterpolationVariables(ns);

				secondaryNamespaces.put(variables.get("prefix"), variables);
			}
		}

		return secondaryNamespaces;
	}

	private Map<String, String> getWorkspaceInterpolationVariables(Namespace ns) {
		String prefix = ns.getPrefix();
		String uri = ns.getUri();
		String workspaceId = prefix + "_workspace";
		String workspaceName = prefix;
		String namespaceId = prefix + "_namespace";

		Map<String, String> variables = new HashMap<String, String>();
		variables.put("prefix", prefix);
		variables.put("uri", uri);
		variables.put("workspaceId", workspaceId);
		variables.put("workspaceName", workspaceName);
		variables.put("namespaceId", namespaceId);

		return variables;
	}

	public Map<String, Map<String, String>> getFeatureTypes() {
		checkMappingGenerated();

		Map<String, Map<String, String>> featureTypes = new HashMap<String, Map<String, String>>();
		for (FeatureTypeMapping ftMapping : mappingWrapper.getAppSchemaMapping().getTypeMappings()
				.getFeatureTypeMapping()) {
			Map<String, String> variables = getFeatureTypeInterpolationVariables(ftMapping);

			featureTypes.put(variables.get("featureTypeName"), variables);
		}

		return featureTypes;
	}

	private Map<String, String> getFeatureTypeInterpolationVariables(FeatureTypeMapping ftMapping) {
		String featureTypeName = stripPrefix(ftMapping.getTargetElement());
		String featureTypeId = featureTypeName + "_featureType";
		String layerName = featureTypeName;
		String layerId = layerName + "_layer";

		Map<String, String> variables = new HashMap<String, String>();
		variables.put("featureTypeId", featureTypeId);
		variables.put("featureTypeName", featureTypeName);
		variables.put("layerId", layerId);
		variables.put("layerName", layerName);

		return variables;
	}

	private void checkMappingGenerated() {
		if (mappingWrapper == null) {
			throw new IllegalStateException("No mapping has been generated yet");
		}
	}

	private void checkTargetSchemaAvailable() {
		if (targetSchema == null) {
			throw new IllegalStateException("Target schema not available");
		}
	}

	private Schema pickTargetSchema() {
		if (this.targetSchemaSpace == null) {
			return null;
		}

		return this.targetSchemaSpace.getSchemas().iterator().next();
	}

	private String extractSchemaName(URI schemaLocation) {
		String path = schemaLocation.getPath();
		String fragment = schemaLocation.getFragment();
		if (fragment != null && !fragment.isEmpty()) {
			path = path.replace(fragment, "");
		}
		int lastSlashIdx = path.lastIndexOf('/');
		int lastDotIdx = path.lastIndexOf('.');
		if (lastSlashIdx >= 0) {
			if (lastDotIdx >= 0) {
				return path.substring(lastSlashIdx + 1, lastDotIdx);
			}
			else {
				// no dot
				return path.substring(lastSlashIdx + 1);
			}
		}
		else {
			// no slash, no dot
			return path;
		}
	}

	private String stripPrefix(String qualifiedName) {
		if (qualifiedName == null) {
			return null;
		}

		String[] prefixAndName = qualifiedName.split(":");
		if (prefixAndName.length == 2) {
			return prefixAndName[1];
		}
		else {
			return null;
		}
	}

	private void applyDataStoreConfig() {
		if (dataStore != null && dataStore.getParameters() != null) {
			DataStore targetDS = mappingWrapper.getDefaultDataStore();

			List<Parameter> inputParameters = dataStore.getParameters().getParameter();
			List<Parameter> targetParameters = targetDS.getParameters().getParameter();
			// update destination parameters
			for (Parameter inputParam : inputParameters) {
				boolean updated = false;
				for (Parameter targetParam : targetParameters) {
					if (inputParam.getName().equals(targetParam.getName())) {
						targetParam.setValue(inputParam.getValue());
						updated = true;
						break;
					}
				}

				if (!updated) {
					// parameter was not already present: add it to the list
					targetParameters.add(inputParam);
				}
			}
		}
	}

	private void createNamespaces() {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
			if (targetEntities != null) {
				for (Entity entity : targetEntities.values()) {
					createNamespaceForEntity(entity, mappingWrapper);
				}
			}

			Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);
			for (Cell propCell : propertyCells) {
				Collection<? extends Entity> targetProperties = propCell.getTarget().values();
				if (targetProperties != null) {
					for (Entity property : targetProperties) {
						createNamespaceForEntity(property, mappingWrapper);
					}
				}
			}
		}
	}

	private void createNamespaceForEntity(Entity entity, AppSchemaMappingWrapper wrapper) {
		QName typeName = entity.getDefinition().getType().getName();
		String namespaceURI = typeName.getNamespaceURI();
		String prefix = typeName.getPrefix();

		wrapper.getOrCreateNamespace(namespaceURI, prefix);

		List<ChildContext> propertyPath = entity.getDefinition().getPropertyPath();
		createNamespacesForPath(propertyPath, wrapper);
	}

	private void createNamespacesForPath(List<ChildContext> propertyPath,
			AppSchemaMappingWrapper wrapper) {
		if (propertyPath != null) {
			for (ChildContext childContext : propertyPath) {
				PropertyDefinition child = childContext.getChild().asProperty();
				if (child != null) {
					String namespaceURI = child.getName().getNamespaceURI();
					String prefix = child.getName().getPrefix();

					wrapper.getOrCreateNamespace(namespaceURI, prefix);
				}
			}
		}
	}

	private void createTargetTypes() {
		Iterable<? extends Schema> targetSchemas = targetSchemaSpace.getSchemas();
		if (targetSchemas != null) {
			for (Schema targetSchema : targetSchemas) {
				mappingWrapper.addSchemaURI(targetSchema.getLocation().toString());
			}
		}
	}

	private void createTypeMappings(IOReporter reporter) {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			String typeTransformId = typeCell.getTransformationIdentifier();
			TypeTransformationHandler typeTransformHandler = null;

			try {
				typeTransformHandler = TypeTransformationHandlerFactory.getInstance()
						.createTypeTransformationHandler(typeTransformId);
				FeatureTypeMapping ftMapping = typeTransformHandler.handleTypeTransformation(
						alignment, typeCell, mappingWrapper);

				if (ftMapping != null) {
					Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);
					for (Cell propertyCell : propertyCells) {
						String propertyTransformId = propertyCell.getTransformationIdentifier();
						PropertyTransformationHandler propertyTransformHandler = null;

						try {
							propertyTransformHandler = PropertyTransformationHandlerFactory
									.getInstance().createPropertyTransformationHandler(
											propertyTransformId);
							propertyTransformHandler.handlePropertyTransformation(propertyCell,
									mappingWrapper);
						} catch (UnsupportedTransformationException e) {
							String errMsg = MessageFormat.format(
									"Error processing property cell{0}", propertyCell.getId());
							log.warn(errMsg, e);
							if (reporter != null) {
								reporter.warn(new IOMessageImpl(errMsg, e));
							}
						}
					}
				}
			} catch (UnsupportedTransformationException e) {
				String errMsg = MessageFormat.format("Error processing type cell{0}",
						typeCell.getId());
				log.warn(errMsg, e);
				if (reporter != null) {
					reporter.warn(new IOMessageImpl(errMsg, e));
				}
			}
		}
	}

	private AppSchemaDataAccessType loadMappingTemplate() throws IOException {
		InputStream is = getClass().getResourceAsStream(AppSchemaIO.MAPPING_TEMPLATE);

		JAXBElement<AppSchemaDataAccessType> templateElement = null;
		try {
			JAXBContext context = createJaxbContext();
			Unmarshaller unmarshaller = context.createUnmarshaller();

			templateElement = unmarshaller.unmarshal(new StreamSource(is),
					AppSchemaDataAccessType.class);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

		return templateElement.getValue();
	}

	public void writeMappingConf(OutputStream out) throws IOException {
		checkMappingGenerated();

		try {
			JAXBContext context = createJaxbContext();

			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			JAXBElement<AppSchemaDataAccessType> mappingConfElement = new ObjectFactory()
					.createAppSchemaDataAccess(mappingWrapper.getAppSchemaMapping());

			marshaller.marshal(mappingConfElement, out);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

	}

	private JAXBContext createJaxbContext() throws JAXBException {
		JAXBContext context = JAXBContext
				.newInstance("net.opengis.gml:net.opengis.ogc:org.geotools.app_schema");

		return context;
	}
}
