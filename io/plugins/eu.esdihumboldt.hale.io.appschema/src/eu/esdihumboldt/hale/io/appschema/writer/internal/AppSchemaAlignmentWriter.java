package eu.esdihumboldt.hale.io.appschema.writer.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.ObjectFactory;
import org.geotools.app_schema.TargetTypesPropertyType;
import org.geotools.app_schema.TypeMappingsPropertyType;
import org.geotools.app_schema.TargetTypesPropertyType.FeatureType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentWriter;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.TransformationMode;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
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
			
			// populate targetTypes element
			Iterable<? extends Schema> targetSchemas = getTargetSchema().getSchemas();
			if (targetSchemas != null) {
				TargetTypesPropertyType targetTypes = mappingTemplate.getTargetTypes();
				FeatureType featureType = targetTypes.getFeatureType();
				if (featureType == null) {
					featureType = new FeatureType();
					targetTypes.setFeatureType(featureType);
				}
				List<String> schemaUris = featureType.getSchemaUri();
				for (Schema targetSchema: targetSchemas) {
					schemaUris.add(targetSchema.getLocation().toString());
				}
			}
			
			// populate typeMappings element
			TypeMappingsPropertyType typeMappings = mappingTemplate.getTypeMappings();
			List<FeatureTypeMapping> featureTypeMappings = typeMappings.getFeatureTypeMapping();
			Collection<? extends Cell> typeCells = getAlignment().getTypeCells();
			for (Cell typeCell: typeCells) {
				// TODO: I have no idea what the keys mean in these ListMultimaps...
				ListMultimap<String, ? extends Entity> typeSourceEntities = typeCell.getSource();
				ListMultimap<String, ? extends Entity> typeTargetEntities = typeCell.getTarget();
				if (typeSourceEntities.size() > 1 || typeTargetEntities.size() > 1) {
					throw new IllegalStateException("Only 1:1 type mappings are supported so far");
				}
				
				Entity sourceType = typeSourceEntities.values().iterator().next();
				Entity targetType = typeTargetEntities.values().iterator().next();
				FeatureTypeMapping featureTypeMapping = new FeatureTypeMapping();
				featureTypeMapping.setSourceDataStore("datastore"); // TODO: make this parametric
				featureTypeMapping.setSourceType(sourceType.getDefinition()
						.getType().getName().getLocalPart());
				featureTypeMapping.setTargetElement(targetType.getDefinition()
						.getType().getName().getPrefix()
						+ ":"
						+ targetType.getDefinition().getType().getName().getLocalPart());
				featureTypeMappings.add(featureTypeMapping);
			}
			
			writeMappingConf(mappingTemplate, System.out);
		} catch (Exception e) {
			reporter.error(new IOMessageImpl(e.getMessage(), e));
			reporter.setSuccess(false);
			return reporter;
		}
		
//		Collection<? extends Cell> typeCells = getAlignment().getTypeCells();
//		for (Cell typeCell: typeCells) {
//			String typeCellId = typeCell.getId();
//			log.info("typeCellId: {}", typeCellId);
//			String typeTransformationId = typeCell.getTransformationIdentifier();
//			log.info("typeTransformationId: {}", typeTransformationId);
//			TransformationMode transfMode = typeCell.getTransformationMode();
//			log.info("trasnfMode: {}", transfMode);
//			ListMultimap<String, ParameterValue> typeTransformParams = typeCell.getTransformationParameters();
//			log.info("typeTransformParameters:");
//			logParameters(typeTransformParams);
//			ListMultimap<String, ? extends Entity> typeSourceEntities = typeCell.getSource();
//			log.info("typeSourceEntities:");
//			logEntities(typeSourceEntities);
//			ListMultimap<String, ? extends Entity> typeTargetEntities = typeCell.getTarget();
//			log.info("typeTargetEntities:");
//			logEntities(typeTargetEntities);
//			
//			Collection<? extends Cell> propertyCells = getAlignment().getPropertyCells(typeCell);
//			for (Cell propertyCell: propertyCells) {
//				String propertyCellId = propertyCell.getId();
//				log.info("propertyCellId: {}", propertyCellId);
//				String propertyTransformationId = propertyCell.getTransformationIdentifier();
//				log.info("propertyTransformationId: {}", propertyTransformationId);
//				ListMultimap<String, ParameterValue> propertyTransformParams = propertyCell.getTransformationParameters();
//				log.info("propertyTransformParameters:");
//				logParameters(propertyTransformParams);
//				ListMultimap<String, ? extends Entity> propertySourceEntities = propertyCell.getSource();
//				log.info("propertySourceEntities:");
//				logEntities(propertySourceEntities);
//				ListMultimap<String, ? extends Entity> propertyTargetEntities = propertyCell.getTarget();
//				log.info("propertyTargetEntities:");
//				logEntities(propertyTargetEntities);
//			}
//		}
		
		progress.end();
		reporter.setSuccess(true);
		return reporter;
	}

	@Override
	protected String getDefaultTypeName() {
		return "GeoTools app-schema";
	}
	
	private void logEntities(ListMultimap<String, ? extends Entity> entities) {
		for (Entry<String, ? extends Entity> entry: entities.entries()) {
			log.info("key: {}; entity: {}", entry.getKey(), entry.getValue());
		}
	}
	
	private void logParameters(ListMultimap<String, ParameterValue> parameters) {
		for (Entry<String, ParameterValue> entry: parameters.entries()) {
			log.info("key: {}; entity: {}", entry.getKey(), entry.getValue().getStringRepresentation());
		}
	}
	
	private AppSchemaDataAccessType loadMappingTemplate() throws JAXBException {
		InputStream is = getClass().getResourceAsStream(MAPPING_TEMPLATE_RESOURCE);
		
		JAXBContext context = createJaxbContext();
		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		JAXBElement<AppSchemaDataAccessType> templateElement = unmarshaller
				.unmarshal(new StreamSource(is), AppSchemaDataAccessType.class);
		
		return templateElement.getValue();
	}
	
	private void writeMappingConf(AppSchemaDataAccessType mappingConf, OutputStream out) throws JAXBException {
		JAXBContext context = createJaxbContext();
		
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		JAXBElement<AppSchemaDataAccessType> mappingConfElement = new ObjectFactory()
				.createAppSchemaDataAccess(mappingConf);
		
		marshaller.marshal(mappingConfElement, out);
	}
	
	private JAXBContext createJaxbContext() throws JAXBException {
		JAXBContext context = JAXBContext.newInstance("net.opengis.gml:net.opengis.ogc:org.geotools.app_schema");
		
		return context;
	}

}
