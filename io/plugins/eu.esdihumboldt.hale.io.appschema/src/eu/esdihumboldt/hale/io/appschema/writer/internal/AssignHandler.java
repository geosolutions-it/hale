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

import static eu.esdihumboldt.hale.common.align.model.functions.AssignFunction.ENTITY_ANCHOR;
import static eu.esdihumboldt.hale.common.align.model.functions.AssignFunction.PARAMETER_VALUE;

import java.util.List;

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
 * @author stefano
 */
public class AssignHandler extends AbstractPropertyTransformationHandler {

	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			FeatureTypeMapping featureTypeMapping, AppSchemaMappingContext context) {

		AttributeMappingType attributeMapping = new AttributeMappingType();

		ListMultimap<String, ParameterValue> parameters = propertyCell
				.getTransformationParameters();
		List<ParameterValue> valueParams = parameters.get(PARAMETER_VALUE);
		String value = valueParams.get(0).getStringRepresentation();

		ListMultimap<String, ? extends Entity> sourceEntities = propertyCell.getSource();
		Property anchor = null;
		if (sourceEntities != null && sourceEntities.containsKey(ENTITY_ANCHOR)) {
			anchor = (Property) sourceEntities.get(ENTITY_ANCHOR).get(0);
		}

		Property target = AppSchemaMappingUtils.getTargetProperty(propertyCell);

		// set source attribute
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		String ocql = null;
		if (anchor != null) {
			// TODO: generalize this code
			String anchorAttr = anchor.getDefinition().getDefinition().getName().getLocalPart();
			ocql = "if_then_else(isNull(" + anchorAttr + "), Expression.NIL, '" + value + "')";
		}
		else {
			ocql = "'" + value + "'";
		}
		sourceExpression.setOCQL(ocql);
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
