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
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.getContainerPropertyPath;
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.getTargetProperty;

import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.app_schema.AttributeExpressionMappingType;
import org.geotools.app_schema.AttributeMappingType;
import org.geotools.app_schema.AttributeMappingType.ClientProperty;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class AbstractPropertyTransformationHandler implements
		PropertyTransformationHandler {

	protected AppSchemaMappingContext context;
	protected Cell propertyCell;
	protected Property targetProperty;

	protected FeatureTypeMapping featureTypeMapping;
	protected AttributeMappingType attributeMapping;

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping,
	 *      eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingContext)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			AppSchemaMappingContext context) {
		this.context = context;
		this.propertyCell = propertyCell;
		// TODO: does this hold for any transformation function?
		this.targetProperty = getTargetProperty(propertyCell);

		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();
		TypeDefinition targetPropertyType = targetPropertyDef.getPropertyType();
		TypeDefinition featureType = findOwningFeatureType(targetPropertyEntityDef);

		// in a well-formed mapping, should always be != null
		if (featureType != null) {
			// fetch FeatureTypeMapping from context
			this.featureTypeMapping = context.getOrCreateFeatureTypeMapping(featureType);

			String cqlValue = getSourceExpressionAsCQL();

			// fetch AttributeMappingType from context
			if (!AppSchemaMappingUtils.isXmlAttribute(targetPropertyDef)) {
				attributeMapping = context.getOrCreateAttributeMapping(targetPropertyEntityDef
						.getPropertyPath());

				List<ChildContext> targetPropertyPath = null;
				if (AppSchemaMappingUtils.isGeometryType(targetPropertyType)) {
					// GeometryTypes require special handling
					targetPropertyPath = getContainerPropertyPath(targetPropertyEntityDef
							.getPropertyPath());

					QName geomTypeName = targetPropertyType.getName();
					Namespace geomNS = context.getOrCreateNamespace(geomTypeName.getNamespaceURI(),
							geomTypeName.getPrefix());
					attributeMapping.setTargetAttributeNode(geomNS.getPrefix() + ":"
							+ geomTypeName.getLocalPart());
				}
				else {
					targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
				}

				// set target attribute
				attributeMapping
						.setTargetAttribute(context.buildAttributeXPath(targetPropertyPath));
				// set source expression
				AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
				// TODO: is this general enough?
				sourceExpression.setOCQL(cqlValue);
				attributeMapping.setSourceExpression(sourceExpression);
				if (AppSchemaMappingUtils.isMultiple(targetPropertyDef)) {
					attributeMapping.setIsMultiple(true);
				}
				// TODO: idExpression?
				// TODO: isList?
				// TODO: targetAttributeNode?
				// TODO: encodeIfEmpty?
			}
			else {
				// fetch attribute mapping for parent property
				List<ChildContext> propertyPath = targetPropertyEntityDef.getPropertyPath();
				int parentPropertyIdx = propertyPath.size() - 2;
				if (parentPropertyIdx >= 0) {
					PropertyDefinition parentPropertyDef = propertyPath.get(parentPropertyIdx)
							.getChild().asProperty();
					List<ChildContext> parentPropertyPath = propertyPath.subList(0,
							parentPropertyIdx + 1);
					if (parentPropertyDef != null) {
						attributeMapping = context.getOrCreateAttributeMapping(parentPropertyPath);
						// set targetAttribute if empty
						if (attributeMapping.getTargetAttribute() == null
								|| attributeMapping.getTargetAttribute().isEmpty()) {
							attributeMapping.setTargetAttribute(context
									.buildAttributeXPath(parentPropertyPath));
						}

						Namespace parentPropNS = context.getOrCreateNamespace(parentPropertyDef
								.getName().getNamespaceURI(), parentPropertyDef.getName()
								.getPrefix());
						Namespace targetPropNS = context.getOrCreateNamespace(targetPropertyDef
								.getName().getNamespaceURI(), targetPropertyDef.getName()
								.getPrefix());
						String unqualifiedName = targetPropertyDef.getName().getLocalPart();
						boolean isQualified = !parentPropNS.getUri().equals(targetPropNS.getUri());

						// encode attribute as <ClientProperty>
						ClientProperty clientProperty = new ClientProperty();
						String clientPropName = (isQualified) ? targetPropNS.getPrefix() + ":"
								+ unqualifiedName : unqualifiedName;
						clientProperty.setName(clientPropName);
						clientProperty.setValue(cqlValue);

						attributeMapping.getClientProperty().add(clientProperty);
					}

				}
			}
		}

		return attributeMapping;
	}

//	protected abstract AttributeMappingType handlePropertyTransformation(Cell propertyCell,
//			FeatureTypeMapping featureTypeMapping, AppSchemaMappingContext context);

	protected abstract String getSourceExpressionAsCQL();

}
