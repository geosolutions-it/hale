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

import static eu.esdihumboldt.hale.common.align.model.functions.RenameFunction.PARAMETER_IGNORE_NAMESPACES;
import static eu.esdihumboldt.hale.common.align.model.functions.RenameFunction.PARAMETER_STRUCTURAL_RENAME;

import java.util.List;

import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.AttributeExpressionMappingType;
import org.geotools.app_schema.AttributeMappingType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;

/**
 * TODO Type description
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class RenameHandler implements PropertyTransformationHandler {

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping,
	 *      org.geotools.app_schema.AppSchemaDataAccessType)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			FeatureTypeMapping featureTypeMapping, AppSchemaDataAccessType appSchemaConfiguration) {

		AttributeMappingType attributeMapping = new AttributeMappingType();

		ListMultimap<String, ParameterValue> parameters = propertyCell
				.getTransformationParameters();
		// TODO: how should I handle these parameters?
		List<ParameterValue> structRenParams = parameters.get(PARAMETER_STRUCTURAL_RENAME);
		List<ParameterValue> ignNamespParams = parameters.get(PARAMETER_IGNORE_NAMESPACES);

		ListMultimap<String, ? extends Entity> sourceEntities = propertyCell.getSource();
		Property source = (Property) sourceEntities.values().iterator().next();

		ListMultimap<String, ? extends Entity> targetEntities = propertyCell.getTarget();
		Property target = (Property) targetEntities.values().iterator().next();

		// TODO: generalize this code
		// set source attribute
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		sourceExpression.setOCQL(source.getDefinition().getDefinition().getName().getLocalPart());
		attributeMapping.setSourceExpression(sourceExpression);
		// TODO: generalize this code
		// set target attribute
		String namespace = target.getDefinition().getDefinition().getName().getNamespaceURI();
		String localPart = target.getDefinition().getDefinition().getName().getLocalPart();
		attributeMapping.setTargetAttribute(namespace + ":" + localPart);

		AttributeMappings attrMappings = featureTypeMapping.getAttributeMappings();
		if (attrMappings == null) {
			attrMappings = new AttributeMappings();
			featureTypeMapping.setAttributeMappings(attrMappings);
		}
		attrMappings.getAttributeMapping().add(attributeMapping);

		return attributeMapping;
	}
}
