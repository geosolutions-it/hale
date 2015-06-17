package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;

public interface TypeTransformationHandler {

	public FeatureTypeMapping handleTypeTransformation(Alignment alignment, Cell typeCell,
			AppSchemaMappingWrapper mapping);

}
