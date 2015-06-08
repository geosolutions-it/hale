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

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.geotools.app_schema.AppSchemaDataAccessType;
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
	private final SchemaSpace targetSchema;
	private final DataStore dataStore;

	public AppSchemaMappingGenerator(Alignment alignment, SchemaSpace targetSchema,
			DataStore dataStore) {
		this.alignment = alignment;
		this.targetSchema = targetSchema;
		this.dataStore = dataStore;
	}

	public AppSchemaMappingWrapper generateMapping(IOReporter reporter) throws JAXBException {
		AppSchemaDataAccessType mapping = loadMappingTemplate();
		AppSchemaMappingWrapper wrapper = new AppSchemaMappingWrapper(mapping);

		// create namespace objects for all target types / properties
		// TODO: this removes all namespaces that were defined in the
		// template file, remove it in production
		mapping.getNamespaces().getNamespace().clear();
		createNamespaces(wrapper);

		// apply datastore configuration, if any
		// TODO: for now, only a single datastore is supported
		applyDataStoreConfig(wrapper);

		// populate targetTypes element
		createTargetTypes(wrapper);

		// populate typeMappings element
		createTypeMappings(wrapper, reporter);

		return wrapper;
	}

	public void generateMapping(OutputStream output, IOReporter reporter) throws JAXBException {
		AppSchemaMappingWrapper mappingWrapper = generateMapping(reporter);;

		writeMappingConf(mappingWrapper.getAppSchemaMapping(), output);
	}

	private void applyDataStoreConfig(AppSchemaMappingWrapper wrapper) {
		if (dataStore != null && dataStore.getParameters() != null) {
			DataStore targetDS = wrapper.getDefaultDataStore();

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

	private void createNamespaces(AppSchemaMappingWrapper wrapper) {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
			if (targetEntities != null) {
				for (Entity entity : targetEntities.values()) {
					createNamespaceForEntity(entity, wrapper);
				}
			}

			Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);
			for (Cell propCell : propertyCells) {
				Collection<? extends Entity> targetProperties = propCell.getTarget().values();
				if (targetProperties != null) {
					for (Entity property : targetProperties) {
						createNamespaceForEntity(property, wrapper);
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

	private void createTargetTypes(AppSchemaMappingWrapper wrapper) {
		Iterable<? extends Schema> targetSchemas = targetSchema.getSchemas();
		if (targetSchemas != null) {
			for (Schema targetSchema : targetSchemas) {
				wrapper.addSchemaURI(targetSchema.getLocation().toString());
			}
		}
	}

	private void createTypeMappings(AppSchemaMappingWrapper wrapper, IOReporter reporter) {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			String typeTransformId = typeCell.getTransformationIdentifier();
			TypeTransformationHandler typeTransformHandler = null;

			try {
				typeTransformHandler = TypeTransformationHandlerFactory.getInstance()
						.createTypeTransformationHandler(typeTransformId);
				FeatureTypeMapping ftMapping = typeTransformHandler.handleTypeTransformation(
						alignment, typeCell, wrapper);

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
									wrapper);
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

	private AppSchemaDataAccessType loadMappingTemplate() throws JAXBException {
		InputStream is = getClass().getResourceAsStream(AppSchemaIO.MAPPING_TEMPLATE);

		JAXBContext context = createJaxbContext();
		Unmarshaller unmarshaller = context.createUnmarshaller();

		JAXBElement<AppSchemaDataAccessType> templateElement = unmarshaller.unmarshal(
				new StreamSource(is), AppSchemaDataAccessType.class);

		return templateElement.getValue();
	}

	public static void writeMappingConf(AppSchemaDataAccessType mappingConf, OutputStream out)
			throws JAXBException {
		JAXBContext context = createJaxbContext();

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		JAXBElement<AppSchemaDataAccessType> mappingConfElement = new ObjectFactory()
				.createAppSchemaDataAccess(mappingConf);

		marshaller.marshal(mappingConfElement, out);
	}

	private static JAXBContext createJaxbContext() throws JAXBException {
		JAXBContext context = JAXBContext
				.newInstance("net.opengis.gml:net.opengis.ogc:org.geotools.app_schema");

		return context;
	}
}
