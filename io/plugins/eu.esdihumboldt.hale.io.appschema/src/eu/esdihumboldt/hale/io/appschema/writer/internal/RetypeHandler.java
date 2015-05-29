package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

public class RetypeHandler implements TypeTransformationHandler {

	@Override
	public FeatureTypeMapping handleTypeTransformation(Cell typeCell,
			AppSchemaMappingContext context) {

		// TODO: I have no idea what the keys mean in these
		// ListMultimaps...
		ListMultimap<String, ? extends Entity> typeSourceEntities = typeCell.getSource();
		ListMultimap<String, ? extends Entity> typeTargetEntities = typeCell.getTarget();
		if (typeSourceEntities.size() > 1 || typeTargetEntities.size() > 1) {
			throw new IllegalStateException("Only 1:1 type mappings are supported so far");
		}

		Entity sourceType = typeSourceEntities.values().iterator().next();
		Entity targetType = typeTargetEntities.values().iterator().next();
		TypeDefinition targetTypeDef = targetType.getDefinition().getType();

		FeatureTypeMapping ftMapping = context.getOrCreateFeatureTypeMapping(targetTypeDef);
		// TODO: how do I know the datasource from which data will be read?
		ftMapping.setSourceDataStore("datastore");
		ftMapping.setSourceType(sourceType.getDefinition().getType().getName().getLocalPart());

		return ftMapping;
	}

}
