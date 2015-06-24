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
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.isGeometryType;
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.isGmlId;
import static eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingUtils.isXmlAttribute;

import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.app_schema.AttributeExpressionMappingType;
import org.geotools.app_schema.AttributeMappingType;
import org.geotools.app_schema.AttributeMappingType.ClientProperty;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import eu.esdihumboldt.hale.common.align.model.AlignmentUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Condition;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.Definition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public abstract class AbstractPropertyTransformationHandler implements
		PropertyTransformationHandler {

	protected AppSchemaMappingWrapper mapping;
	protected Cell propertyCell;
	protected Property targetProperty;

	protected FeatureTypeMapping featureTypeMapping;
	protected AttributeMappingType attributeMapping;

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping,
	 *      eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingWrapper)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			AppSchemaMappingWrapper mapping) {
		this.mapping = mapping;
		this.propertyCell = propertyCell;
		// TODO: does this hold for any transformation function?
		this.targetProperty = getTargetProperty(propertyCell);

		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();
		TypeDefinition featureType = findOwningFeatureType(targetPropertyEntityDef);

		// in a well-formed mapping, should always be != null
		if (featureType != null) {
			// fetch FeatureTypeMapping from mapping
			this.featureTypeMapping = mapping.getOrCreateFeatureTypeMapping(featureType);

			// fetch AttributeMappingType from mapping
			if (isXmlAttribute(targetPropertyDef)) {
				// gml:id attribute requires special handling
				if (isGmlId(targetPropertyDef)) {
					handleAsGmlId(featureType);
				}
				else {
					handleAsXmlAttribute();
				}
			}
			else {
				handleAsXmlElement();
			}
		}

		return attributeMapping;
	}

	protected void handleAsGmlId(TypeDefinition featureType) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		List<ChildContext> gmlIdPath = targetPropertyEntityDef.getPropertyPath();

		attributeMapping = mapping.getOrCreateAttributeMapping(gmlIdPath);
		// set targetAttribute to feature type qualified name
		attributeMapping.setTargetAttribute(mapping.getOrCreateFeatureTypeMapping(featureType)
				.getTargetElement());
		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL(getSourceExpressionAsCQL());
		// TODO: not sure whether any CQL expression can be used
		// here
		attributeMapping.setIdExpression(idExpression);
	}

	protected void handleAsXmlAttribute() {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();

		// fetch attribute mapping for parent property
		EntityDefinition parentDef = AlignmentUtil.getParent(targetPropertyEntityDef);
		if (parentDef != null) {
			List<ChildContext> parentPropertyPath = parentDef.getPropertyPath();
			PropertyDefinition parentPropertyDef = parentPropertyPath
					.get(parentPropertyPath.size() - 1).getChild().asProperty();
			if (parentPropertyDef != null) {
				attributeMapping = mapping.getOrCreateAttributeMapping(parentPropertyPath);
				// set targetAttribute if empty
				if (attributeMapping.getTargetAttribute() == null
						|| attributeMapping.getTargetAttribute().isEmpty()) {
					attributeMapping.setTargetAttribute(mapping
							.buildAttributeXPath(parentPropertyPath));
				}

				Namespace parentPropNS = mapping.getOrCreateNamespace(parentPropertyDef.getName()
						.getNamespaceURI(), parentPropertyDef.getName().getPrefix());
				Namespace targetPropNS = mapping.getOrCreateNamespace(targetPropertyDef.getName()
						.getNamespaceURI(), targetPropertyDef.getName().getPrefix());
				String unqualifiedName = targetPropertyDef.getName().getLocalPart();
				boolean isQualified = targetPropNS != null
						&& !parentPropNS.getUri().equals(targetPropNS.getUri());

				// encode attribute as <ClientProperty>
				ClientProperty clientProperty = new ClientProperty();
				String clientPropName = (isQualified) ? targetPropNS.getPrefix() + ":"
						+ unqualifiedName : unqualifiedName;
				clientProperty.setName(clientPropName);
				clientProperty.setValue(getSourceExpressionAsCQL());

				attributeMapping.getClientProperty().add(clientProperty);
			}
		}
	}

	protected void handleAsXmlElement() {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();
		TypeDefinition targetPropertyType = targetPropertyDef.getPropertyType();

		attributeMapping = mapping.getOrCreateAttributeMapping(targetPropertyEntityDef
				.getPropertyPath());

		if (isGeometryType(targetPropertyType)) {
			handleXmlElementAsGeometryType();
		}
		else {
			List<ChildContext> targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
			// set target attribute
			attributeMapping.setTargetAttribute(mapping.buildAttributeXPath(targetPropertyPath));
		}

		// set source expression
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		// TODO: is this general enough?
		sourceExpression.setOCQL(getSourceExpressionAsCQL());
		attributeMapping.setSourceExpression(sourceExpression);
		if (AppSchemaMappingUtils.isMultiple(targetPropertyDef)) {
			attributeMapping.setIsMultiple(true);
		}
		// TODO: isList?
		// TODO: targetAttributeNode?
		// TODO: encodeIfEmpty?
	}

	protected void handleXmlElementAsGeometryType() {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		TypeDefinition targetPropertyType = targetPropertyEntityDef.getDefinition()
				.getPropertyType();

		// GeometryTypes require special handling
		QName geomTypeName = targetPropertyType.getName();
		Namespace geomNS = mapping.getOrCreateNamespace(geomTypeName.getNamespaceURI(),
				geomTypeName.getPrefix());
		attributeMapping.setTargetAttributeNode(geomNS.getPrefix() + ":"
				+ geomTypeName.getLocalPart());

		// set target attribute to parent (should be gml:AbstractGeometry)
		// TODO: this is really ugly, but I don't see a better way to do it
		// since HALE renames
		// {http://www.opengis.net/gml/3.2}AbstractGeometry element
		// to
		// {http://www.opengis.net/gml/3.2/AbstractGeometry}choice
		EntityDefinition parentEntityDef = AlignmentUtil.getParent(targetPropertyEntityDef);
		Definition<?> parentDef = parentEntityDef.getDefinition();
		String parentQName = geomNS.getPrefix() + ":" + parentDef.getDisplayName();
		List<ChildContext> targetPropertyPath = parentEntityDef.getPropertyPath();
		attributeMapping.setTargetAttribute(mapping.buildAttributeXPath(targetPropertyPath) + "/"
				+ parentQName);
	}

	protected static String getConditionalExpression(PropertyEntityDefinition propertyEntityDef,
			String cql) {
		if (propertyEntityDef != null) {
			String propertyName = propertyEntityDef.getDefinition().getName().getLocalPart();
			List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();
			// TODO: conditions are supported only on simple (not nested)
			// properties
			if (propertyPath.size() == 1) {
				Condition condition = propertyPath.get(0).getCondition();
				if (condition != null) {
					String fitlerText = AlignmentUtil.getFilterText(condition.getFilter());
					// remove "parent" references
					fitlerText = fitlerText.replace("parent.", "");
					// replace "value" references with the local name of the
					// property itself
					fitlerText = fitlerText.replace("value", propertyName);

					return String.format("if_then_else(%s, %s, Expression.NIL)", fitlerText, cql);
				}
			}
		}

		return cql;
	}

	protected abstract String getSourceExpressionAsCQL();

}
