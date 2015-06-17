package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;

public class MergeHandler extends SingleSourceToTargetHandler {

	@Override
	public FeatureTypeMapping handleTypeTransformation(Alignment alignment, Cell typeCell,
			AppSchemaMappingWrapper mapping) {
		FeatureTypeMapping ftMapping = super.handleTypeTransformation(alignment, typeCell, mapping);

		// this is the only variation from RetypeHandler so far
		ftMapping.setIsDenormalised(true);

		return ftMapping;
	}

}
