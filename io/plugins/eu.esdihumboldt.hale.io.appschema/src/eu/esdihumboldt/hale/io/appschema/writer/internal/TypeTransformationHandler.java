package eu.esdihumboldt.hale.io.appschema.writer.internal;

import org.geotools.app_schema.AppSchemaDataAccessType;

import eu.esdihumboldt.hale.common.align.model.Cell;

public interface TypeTransformationHandler {
	
	public void handleTypeTransformation(Cell typeCell, AppSchemaDataAccessType appSchemaConfiguration);

}
