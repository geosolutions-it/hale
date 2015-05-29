package eu.esdihumboldt.hale.io.appschema.writer.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.ObjectFactory;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentWriter;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.Schema;

public class AppSchemaAlignmentWriter extends AbstractAlignmentWriter {

	private static final ALogger log = ALoggerFactory.getLogger(AppSchemaAlignmentWriter.class);

	private static final String MAPPING_TEMPLATE_RESOURCE = "/template/app-schema-mapping-template.xml";

	@Override
	public boolean isCancelable() {
		return false;
	}

	@Override
	protected IOReport execute(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		progress.begin("Export HALE alignment as GeoTools app-schema", ProgressIndicator.UNKNOWN);

		try {
			AppSchemaDataAccessType mappingTemplate = loadMappingTemplate();
			AppSchemaMappingContext context = new AppSchemaMappingContext(getAlignment(),
					mappingTemplate);

			// TODO: this removes all namespaces that were defined in the
			// template file, remove it in production
			mappingTemplate.getNamespaces().getNamespace().clear();

			// 1st pass: create namespace objects for all target types /
			// properties
			Collection<? extends Cell> typeCells = getAlignment().getTypeCells();
			for (Cell typeCell : typeCells) {
				ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
				if (targetEntities != null) {
					for (Entity entity : targetEntities.values()) {
						createNamespaceForEntity(entity, context);
					}
				}

				Collection<? extends Cell> propertyCells = getAlignment()
						.getPropertyCells(typeCell);
				for (Cell propCell : propertyCells) {
					Collection<? extends Entity> targetProperties = propCell.getTarget().values();
					if (targetProperties != null) {
						for (Entity property : targetProperties) {
							createNamespaceForEntity(property, context);
						}
					}
				}
			}

			Iterable<? extends Schema> targetSchemas = getTargetSchema().getSchemas();
			// populate targetTypes element
			if (targetSchemas != null) {
				for (Schema targetSchema : targetSchemas) {
					context.addSchemaURI(targetSchema.getLocation().toString());
				}
			}

			// 2nd pass: populate typeMappings element
			for (Cell typeCell : typeCells) {
				String typeTransformId = typeCell.getTransformationIdentifier();
				TypeTransformationHandler typeTransformHandler = null;

				try {
					typeTransformHandler = TypeTransformationHandlerFactory.getInstance()
							.createTypeTransformationHandler(typeTransformId);
					FeatureTypeMapping ftMapping = typeTransformHandler.handleTypeTransformation(
							typeCell, context);

					if (ftMapping != null) {
						Collection<? extends Cell> propertyCells = getAlignment().getPropertyCells(
								typeCell);
						for (Cell propertyCell : propertyCells) {
							String propertyTransformId = propertyCell.getTransformationIdentifier();
							PropertyTransformationHandler propertyTransformHandler = null;

							try {
								propertyTransformHandler = PropertyTransformationHandlerFactory
										.getInstance().createPropertyTransformationHandler(
												propertyTransformId);
								propertyTransformHandler.handlePropertyTransformation(propertyCell,
										context);
							} catch (UnsupportedTransformationException e) {
								String errMsg = MessageFormat.format(
										"Error processing property cell{0}", propertyCell.getId());
								reporter.warn(new IOMessageImpl(errMsg, e));
							}
						}
					}
				} catch (UnsupportedTransformationException e) {
					String errMsg = MessageFormat.format("Error processing type cell{0}",
							typeCell.getId());
					reporter.warn(new IOMessageImpl(errMsg, e));
				}

			}

			if (log.isDebugEnabled()) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					writeMappingConf(mappingTemplate, bos);

					log.debug("app-schema configuration: " + bos.toString("UTF-8"));
				} finally {
					bos.close();
				}
			}

			if (getTarget() != null) {
				OutputStream out = getTarget().getOutput();
				writeMappingConf(mappingTemplate, out);
				out.flush();
			}
		} catch (Exception e) {
			reporter.error(new IOMessageImpl(e.getMessage(), e));
			reporter.setSuccess(false);
			return reporter;
		}

		progress.end();
		reporter.setSuccess(true);
		return reporter;
	}

	@Override
	protected String getDefaultTypeName() {
		return "GeoTools app-schema";
	}

	private void logEntities(ListMultimap<String, ? extends Entity> entities) {
		for (Entry<String, ? extends Entity> entry : entities.entries()) {
			log.info("key: {}; entity: {}", entry.getKey(), entry.getValue());
		}
	}

	private void logParameters(ListMultimap<String, ParameterValue> parameters) {
		for (Entry<String, ParameterValue> entry : parameters.entries()) {
			log.info("key: {}; entity: {}", entry.getKey(), entry.getValue()
					.getStringRepresentation());
		}
	}

	private void createNamespaceForEntity(Entity entity, AppSchemaMappingContext context) {
		QName typeName = entity.getDefinition().getType().getName();
		String namespaceURI = typeName.getNamespaceURI();
		String prefix = typeName.getPrefix();

		context.getOrCreateNamespace(namespaceURI, prefix);

		List<ChildContext> propertyPath = entity.getDefinition().getPropertyPath();
		createNamespacesForPath(propertyPath, context);
	}

	private void createNamespacesForPath(List<ChildContext> propertyPath,
			AppSchemaMappingContext context) {
		if (propertyPath != null) {
			for (ChildContext childContext : propertyPath) {
				PropertyDefinition child = childContext.getChild().asProperty();
				if (child != null) {
					String namespaceURI = child.getName().getNamespaceURI();
					String prefix = child.getName().getPrefix();

					context.getOrCreateNamespace(namespaceURI, prefix);
				}
			}
		}
	}

	private AppSchemaDataAccessType loadMappingTemplate() throws JAXBException {
		InputStream is = getClass().getResourceAsStream(MAPPING_TEMPLATE_RESOURCE);

		JAXBContext context = createJaxbContext();
		Unmarshaller unmarshaller = context.createUnmarshaller();

		JAXBElement<AppSchemaDataAccessType> templateElement = unmarshaller.unmarshal(
				new StreamSource(is), AppSchemaDataAccessType.class);

		return templateElement.getValue();
	}

	private void writeMappingConf(AppSchemaDataAccessType mappingConf, OutputStream out)
			throws JAXBException {
		JAXBContext context = createJaxbContext();

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		JAXBElement<AppSchemaDataAccessType> mappingConfElement = new ObjectFactory()
				.createAppSchemaDataAccess(mappingConf);

		marshaller.marshal(mappingConfElement, out);
	}

	private JAXBContext createJaxbContext() throws JAXBException {
		JAXBContext context = JAXBContext
				.newInstance("net.opengis.gml:net.opengis.ogc:org.geotools.app_schema");

		return context;
	}

}
