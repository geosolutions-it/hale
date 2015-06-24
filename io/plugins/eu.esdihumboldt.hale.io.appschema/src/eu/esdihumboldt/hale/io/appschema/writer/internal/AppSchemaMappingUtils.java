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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingUtils {

	public static final String GML_BASE_NAMESPACE = "http://www.opengis.net/gml";
	public static final String GML_ID = "id";
	public static final String GML_ABSTRACT_FEATURE_TYPE = "AbstractFeatureType";
	public static final String GML_ABSTRACT_GEOMETRY_TYPE = "AbstractGeometryType";

//	public static final QName QNAME_GML_ID = new QName("http://www.opengis.net/gml/3.2", "id");
//	public static final QName QNAME_ABSTRACT_FEATURE_TYPE = new QName(
//			"http://www.opengis.net/gml/3.2", "AbstractFeatureType");
//	public static final QName QNAME_ABSTRACT_GEOMETRY_TYPE = new QName(
//			"http://www.opengis.net/gml/3.2", "AbstractGeometryType");
	public static final QName QNAME_XLINK_XREF = new QName("http://www.w3.org/1999/xlink", "href");

	public static boolean isGmlId(PropertyDefinition propertyDef) {
		// return propertyDef != null &&
		// propertyDef.getName().equals(QNAME_GML_ID);
		if (propertyDef == null) {
			return false;
		}

		QName propertyName = propertyDef.getName();

		return hasGmlNamespace(propertyName) && GML_ID.equals(propertyName.getLocalPart());
	}

	public static boolean isXmlAttribute(PropertyDefinition propertyDef) {
		XmlAttributeFlag xmlAttrFlag = propertyDef.getConstraint(XmlAttributeFlag.class);

		return xmlAttrFlag != null && xmlAttrFlag.isEnabled();
	}

	public static boolean isFeatureType(TypeDefinition typeDefinition) {
		if (typeDefinition == null) {
			return false;
		}

		QName typeName = typeDefinition.getName();
		if (hasGmlNamespace(typeName) && GML_ABSTRACT_FEATURE_TYPE.equals(typeName.getLocalPart())) {
			return true;
		}
		else {
			return isFeatureType(typeDefinition.getSuperType());
		}
	}

	public static boolean isGeometryType(TypeDefinition typeDefinition) {
		if (typeDefinition == null) {
			return false;
		}

		QName typeName = typeDefinition.getName();
		if (hasGmlNamespace(typeName) && GML_ABSTRACT_GEOMETRY_TYPE.equals(typeName.getLocalPart())) {
			return true;
		}
		else {
			return isGeometryType(typeDefinition.getSuperType());
		}
	}

	private static boolean hasGmlNamespace(QName qname) {
		return qname.getNamespaceURI().startsWith(GML_BASE_NAMESPACE);
	}

	public static boolean isMultiple(PropertyDefinition targetPropertyDef) {
		if (targetPropertyDef != null) {
			Cardinality cardinality = targetPropertyDef.getConstraint(Cardinality.class);
			if (cardinality != null) {
				long maxOccurs = cardinality.getMaxOccurs();
				if (maxOccurs > 1 || maxOccurs == Cardinality.UNBOUNDED) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isXRefAttribute(PropertyDefinition propertyDef) {
		return propertyDef != null && propertyDef.getName().equals(QNAME_XLINK_XREF);
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
//			int lastIdx = ftIdx - 1;
//			// make sure last element is a property and not a group (i.e. a
//			// choice element)
//			while (lastIdx > 0 && propertyPath.get(lastIdx).getChild().asProperty() == null) {
//				lastIdx--;
//			}
//			return propertyPath.subList(0, lastIdx);
			return getContainerPropertyPath(propertyPath.subList(0, ftIdx));
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

	public static TypeDefinition findChildFeatureType(TypeDefinition typeDef) {
		if (typeDef != null) {
			Collection<? extends ChildDefinition<?>> children = typeDef.getChildren();
			if (children != null) {
				for (ChildDefinition<?> child : children) {
					PropertyDefinition childPropertyDef = child.asProperty();
					if (childPropertyDef != null) {
						TypeDefinition childPropertyType = childPropertyDef.getPropertyType();
						if (isFeatureType(childPropertyType)) {
							return childPropertyType;
						}
					}
				}
			}
		}

		return null;
	}

	public static List<ChildContext> getContainerPropertyPath(List<ChildContext> propertyPath) {
		if (propertyPath == null || propertyPath.size() == 0) {
			return Collections.emptyList();
		}

		int lastIdx = propertyPath.size() - 1;
		// make sure last element is a property and not a group (e.g. a choice
		// element)
		while (lastIdx > 0 && propertyPath.get(lastIdx).getChild().asProperty() == null) {
			lastIdx--;
		}
		return propertyPath.subList(0, lastIdx);
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
