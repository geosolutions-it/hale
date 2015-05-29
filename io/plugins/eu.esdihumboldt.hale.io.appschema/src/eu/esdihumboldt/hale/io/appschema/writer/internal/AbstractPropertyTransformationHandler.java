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

package eu.esdihumboldt.hale.io.appschema.writer.internal;

import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.findOwningFeatureType;
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.getTargetProperty;

import org.geotools.app_schema.AttributeMappingType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class AbstractPropertyTransformationHandler implements
		PropertyTransformationHandler {

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping,
	 *      eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingContext)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			AppSchemaMappingContext context) {
		Property targetProperty = getTargetProperty(propertyCell);

		TypeDefinition featureType = findOwningFeatureType(targetProperty.getDefinition());
		// in a well-formed mapping, should always be != null
		if (featureType != null) {
			// fetch FeatureTypeMapping from context
			FeatureTypeMapping ftMapping = context.getOrCreateFeatureTypeMapping(featureType);
			return handlePropertyTransformation(propertyCell, ftMapping, context);
		}

		return null;
	}

	protected abstract AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			FeatureTypeMapping featureTypeMapping, AppSchemaMappingContext context);

}
