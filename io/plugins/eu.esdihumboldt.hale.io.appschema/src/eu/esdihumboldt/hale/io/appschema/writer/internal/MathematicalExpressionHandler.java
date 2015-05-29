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

import static eu.esdihumboldt.cst.functions.numeric.MathematicalExpressionFunction.PARAMETER_EXPRESSION;

import org.geotools.app_schema.AttributeExpressionMappingType;
import org.geotools.app_schema.AttributeMappingType;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class MathematicalExpressionHandler extends AbstractPropertyTransformationHandler {

	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			FeatureTypeMapping featureTypeMapping, AppSchemaMappingContext context) {
		AttributeMappingType attributeMapping = new AttributeMappingType();

		String mathExpression = propertyCell.getTransformationParameters()
				.get(PARAMETER_EXPRESSION).get(0).getStringRepresentation();

		Property target = AppSchemaMappingUtils.getTargetProperty(propertyCell);

		// set source attribute
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		// TODO: verify that math expressions work as-is in CQL
		sourceExpression.setOCQL(mathExpression);
		// TODO: what about idExpression?
		attributeMapping.setSourceExpression(sourceExpression);

		// set target attribute
		String targetAttribute = context.buildAttributeXPath(target.getDefinition());
		attributeMapping.setTargetAttribute(targetAttribute);

		AttributeMappings attrMappings = featureTypeMapping.getAttributeMappings();
		if (attrMappings == null) {
			attrMappings = new AttributeMappings();
			featureTypeMapping.setAttributeMappings(attrMappings);
		}
		attrMappings.getAttributeMapping().add(attributeMapping);

		return attributeMapping;
	}

}
