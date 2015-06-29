package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;

/**
 * Interface defining the API for type transformation handlers.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public interface TypeTransformationHandler {

	/**
	 * Translates a type cell to an app-schema feature type mapping.
	 * 
	 * @param alignment the alignment
	 * @param typeCell the type cell
	 * @param mapping the app-schema mapping wrapper
	 * @return the feature type mapping
	 */
	public FeatureTypeMapping handleTypeTransformation(Alignment alignment, Cell typeCell,
			AppSchemaMappingWrapper mapping);

}
