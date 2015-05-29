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

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.app_schema.AttributeMappingType;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingUtils {

	public static final QName QNAME_ABSTRACT_FEATURE_TYPE = new QName(
			"http://www.opengis.net/gml/3.2", "AbstractFeatureType");

	public static void setIsMultiple(PropertyDefinition targetPropertyDef,
			AttributeMappingType attrMapping) {
		if (targetPropertyDef != null && attrMapping != null) {
			Cardinality cardinality = targetPropertyDef.getConstraint(Cardinality.class);
			if (cardinality != null) {
				long maxOccurs = cardinality.getMaxOccurs();
				if (maxOccurs > 1 || maxOccurs == Cardinality.UNBOUNDED) {
					attrMapping.setIsMultiple(true);
				}
			}
		}
	}

	public static TypeDefinition findOwningFeatureType(PropertyEntityDefinition propertyEntityDef) {
		List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();

		return findOwningFeatureType(propertyPath);
	}

	public static TypeDefinition findOwningFeatureType(List<ChildContext> propertyPath) {
		int ftIdx = findOwningFeatureTypeIndex(propertyPath);

		if (ftIdx >= 0) {
			return propertyPath.get(ftIdx).getChild().asProperty().getParentType();
		}
		else {
			return null;
		}
	}

	public static List<ChildContext> findOwningFeatureTypePath(
			PropertyEntityDefinition propertyEntityDef) {
		List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();

		return findOwningFeatureTypePath(propertyPath);
	}

	public static List<ChildContext> findOwningFeatureTypePath(List<ChildContext> propertyPath) {
		int ftIdx = findOwningFeatureTypeIndex(propertyPath);

		if (ftIdx >= 0) {
			int lastIdx = ftIdx - 1;
			// make sure last element is a property and not a group (i.e. a
			// choice element)
			while (lastIdx > 0 && propertyPath.get(lastIdx).getChild().asProperty() == null) {
				lastIdx--;
			}
			return propertyPath.subList(0, lastIdx);
		}

		return Collections.emptyList();
	}

	private static int findOwningFeatureTypeIndex(List<ChildContext> propertyPath) {
		for (int i = propertyPath.size() - 1; i >= 0; i--) {
			ChildContext childContext = propertyPath.get(i);
			PropertyDefinition propertyDef = childContext.getChild().asProperty();
			if (propertyDef != null) {
				TypeDefinition parentType = propertyDef.getParentType();
				if (isFeatureType(parentType)) {
					return i;
				}
			}
		}

		return -1;
	}

	public static boolean isFeatureType(TypeDefinition typeDefinition) {
		if (typeDefinition == null) {
			return false;
		}

		if (typeDefinition.getName().equals(QNAME_ABSTRACT_FEATURE_TYPE)) {
			return true;
		}
		else {
			return isFeatureType(typeDefinition.getSuperType());
		}
	}

	/**
	 * Return the first target {@link Entity}, which is assumed to be a
	 * {@link Property}.
	 * 
	 * @param propertyCell
	 * @return the target {@link Property}
	 */
	public static Property getTargetProperty(Cell propertyCell) {
		ListMultimap<String, ? extends Entity> targetEntities = propertyCell.getTarget();
		if (targetEntities != null && !targetEntities.isEmpty()) {
			return (Property) targetEntities.values().iterator().next();
		}

		return null;
	}

	/**
	 * Return the first source {@link Entity}, which is assumed to be a
	 * {@link Property}.
	 * 
	 * @param propertyCell
	 * @return the target {@link Property}
	 */
	public static Property getSourceProperty(Cell propertyCell) {
		ListMultimap<String, ? extends Entity> sourceEntities = propertyCell.getSource();
		if (sourceEntities != null && !sourceEntities.isEmpty()) {
			return (Property) sourceEntities.values().iterator().next();
		}

		return null;
	}
}
