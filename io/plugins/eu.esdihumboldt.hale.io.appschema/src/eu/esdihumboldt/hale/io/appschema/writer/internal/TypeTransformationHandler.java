package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Cell;

public interface TypeTransformationHandler {

	public FeatureTypeMapping handleTypeTransformation(Cell typeCell,
			AppSchemaDataAccessType appSchemaConfiguration);

}
