/*
 * Copyright (c) 2012 Data Harmonisation Panel
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
 *     HUMBOLDT EU Integrated Project #030962
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */
package eu.esdihumboldt.hale.io.shp.reader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOProvider;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.instance.model.Instance;
import eu.esdihumboldt.hale.common.schema.geometry.GeometryProperty;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.NillableFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.AbstractFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.Binding;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.HasValueFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappableFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappingRelevantFlag;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchema;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition;
import eu.esdihumboldt.hale.common.schema.persist.AbstractCachedSchemaReader;
import eu.esdihumboldt.hale.io.shp.ShapefileConstants;
import eu.esdihumboldt.hale.io.shp.internal.Messages;

/**
 * Reads a schema from a shapefile.
 * 
 * @author Thorsten Reitz
 * @author Simon Templer
 */
public class ShapeSchemaReader extends AbstractCachedSchemaReader implements ShapefileConstants {

	@Override
	protected Schema loadFromSource(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		progress.begin(Messages.getString("ShapeSchemaProvider.1"), ProgressIndicator.UNKNOWN); //$NON-NLS-1$

//		DataStore store = new ShapefileDataStoreFactory().createDataStore(location.toURL());
//		DataStore store = FileDataStoreFinder.getDataStore(getSource().getLocation().toURL());

		ShapefileDataStore store = new ShapefileDataStore(getSource().getLocation().toURL(), false,
				getCharset());

		// TODO namespace from configuration parameter?!
		String namespace = ShapefileConstants.SHAPEFILE_NS;
		DefaultSchema schema = new DefaultSchema(namespace, getSource().getLocation());

		progress.setCurrentTask(Messages.getString("ShapeSchemaProvider.2")); //$NON-NLS-1$

		// create type for augmented filename property
		QName filenameTypeName = new QName(SHAPEFILE_AUGMENT_NS, "filenameType");
		TypeDefinition filenameType = null;
		if (getSharedTypes() != null) {
			filenameType = getSharedTypes().getType(filenameTypeName);
		}
		if (filenameType == null) {
			DefaultTypeDefinition fnt = new DefaultTypeDefinition(filenameTypeName);

			fnt.setConstraint(MappableFlag.DISABLED);
			fnt.setConstraint(MappingRelevantFlag.DISABLED);
			fnt.setConstraint(Binding.get(String.class));
			fnt.setConstraint(HasValueFlag.ENABLED);

			filenameType = fnt;

			schema.addType(filenameType);
		}

		// create type for augmented generic geometry property
		QName genGeomTypeName = new QName(SHAPEFILE_AUGMENT_NS, "geomType");
		TypeDefinition genGeomType = null;
		if (getSharedTypes() != null) {
			genGeomType = getSharedTypes().getType(genGeomTypeName);
		}
		if (genGeomType == null) {
			DefaultTypeDefinition ggt = new DefaultTypeDefinition(genGeomTypeName);

			ggt.setConstraint(MappableFlag.DISABLED);
			ggt.setConstraint(MappingRelevantFlag.DISABLED);
			ggt.setConstraint(HasValueFlag.ENABLED);
			ggt.setConstraint(Binding.get(GeometryProperty.class));

			genGeomType = ggt;

			schema.addType(genGeomType);
		}

		// shared (artificial) base type for ShapeFiles
		QName baseTypeName = new QName(SHAPEFILE_AUGMENT_NS, "AbstractShapeFeature");
		TypeDefinition baseType = null;
		if (getSharedTypes() != null) {
			baseType = getSharedTypes().getType(baseTypeName);
		}
		if (baseType == null) {
			DefaultTypeDefinition bt = new DefaultTypeDefinition(baseTypeName);

			bt.setConstraint(MappableFlag.DISABLED);
			bt.setConstraint(MappingRelevantFlag.DISABLED);
			bt.setConstraint(HasValueFlag.DISABLED);
			bt.setConstraint(AbstractFlag.ENABLED);

			// populate base type

			// add additional filename property
//			String filename = sft.getName().getLocalPart();
			DefaultPropertyDefinition property = new DefaultPropertyDefinition(new QName(
					SHAPEFILE_AUGMENT_NS, AUGMENTED_PROPERTY_FILENAME), bt, filenameType);
			property.setConstraint(Cardinality.CC_EXACTLY_ONCE);
			property.setConstraint(NillableFlag.ENABLED);

			// generic geometry property (XXX can't be overriden currently ->
			// would result in a duplicate)
			DefaultPropertyDefinition geomProperty = new DefaultPropertyDefinition(new QName(
					"the_geom"), bt, genGeomType);
			// set constraints on property
			geomProperty.setConstraint(NillableFlag.ENABLED); // nillable
			geomProperty.setConstraint(Cardinality.CC_OPTIONAL); // cardinality

			// FIXME ID1 property
			DefaultTypeDefinition id1Type = new DefaultTypeDefinition(new QName(
					SHAPEFILE_AUGMENT_NS, "ID1Type"));
			id1Type.setConstraint(MappableFlag.DISABLED);
			id1Type.setConstraint(MappingRelevantFlag.DISABLED);
			id1Type.setConstraint(HasValueFlag.ENABLED);
			id1Type.setConstraint(Binding.get(Long.class));
			DefaultPropertyDefinition id1Property = new DefaultPropertyDefinition(new QName("ID1"),
					bt, id1Type);
			// set constraints on property
			id1Property.setConstraint(NillableFlag.ENABLED); // nillable
			id1Property.setConstraint(Cardinality.CC_OPTIONAL); // cardinality

			baseType = bt;

			schema.addType(baseType);
		}

		// build type definitions based on Schema extracted by geotools
		for (Name name : store.getNames()) {
			SimpleFeatureType sft = store.getSchema(name);
			try {
				// create type definition
				DefaultTypeDefinition type = new DefaultTypeDefinition(new QName(namespace, sft
						.getName().getLocalPart()));
				type.setSuperType((DefaultTypeDefinition) baseType);

				// constraints on main type
				type.setConstraint(MappingRelevantFlag.ENABLED);
				type.setConstraint(MappableFlag.ENABLED);
				type.setConstraint(HasValueFlag.DISABLED);
				type.setConstraint(AbstractFlag.DISABLED);
				type.setConstraint(Binding.get(Instance.class));

				for (AttributeDescriptor ad : sft.getAttributeDescriptors()) {
					QName propertyName = new QName(ad.getLocalName());

					if (baseType.getChild(propertyName) == null) {
						// only add property if not yet present in the base type

						DefaultPropertyDefinition property = new DefaultPropertyDefinition(
								propertyName, type, getTypeFromAttributeType(ad.getType(), schema,
										namespace));

						// set constraints on property
						property.setConstraint(NillableFlag.get(ad.isNillable())); // nillable
						property.setConstraint(Cardinality.get(ad.getMinOccurs(), ad.getMaxOccurs())); // cardinality

						// set metadata
						property.setLocation(getSource().getLocation());
					}
				}

				schema.addType(type);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			progress.setCurrentTask(MessageFormat.format(
					Messages.getString("ShapeSchemaProvider.7"), //$NON-NLS-1$
					sft.getTypeName()));
		}

		reporter.setSuccess(true);
		return schema;
	}

	@Override
	protected Charset getDefaultCharset() {
		// default charset: ISO-8859-1
		return StandardCharsets.ISO_8859_1;
	}

	/**
	 * Create a type definition for a simple attribute type
	 * 
	 * @param type the attribute type
	 * @param schema the schema
	 * @param namespace the namespace to use for the type definition
	 * @return the type definition
	 */
	private TypeDefinition getTypeFromAttributeType(AttributeType type, DefaultSchema schema,
			String namespace) {
		QName typeName = new QName(namespace, type.getName().getLocalPart());
		TypeDefinition result = null;

		// check shared types
		if (getSharedTypes() != null) {
			result = getSharedTypes().getType(typeName);
		}

		if (result == null) {
			// get type from schema
			result = schema.getType(typeName);
		}

		if (result == null) {
			// create new type
			DefaultTypeDefinition typeDef = new DefaultTypeDefinition(typeName);

			// set constraints
			typeDef.setConstraint(MappingRelevantFlag.DISABLED); // not mappable
			typeDef.setConstraint(MappableFlag.DISABLED);
			// binding
			if (Geometry.class.isAssignableFrom(type.getBinding())) {
				// create geometry binding
				typeDef.setConstraint(Binding.get(GeometryProperty.class));
			}
			else {
				typeDef.setConstraint(Binding.get(type.getBinding()));
			}
			typeDef.setConstraint(HasValueFlag.ENABLED); // simple type

			// set metadata
			typeDef.setLocation(getSource().getLocation());
			if (type.getDescription() != null) {
				typeDef.setDescription(type.getDescription().toString());
			}

			result = typeDef;
			schema.addType(result);
		}

		return result;
	}

	/**
	 * @see AbstractIOProvider#getDefaultTypeName()
	 */
	@Override
	protected String getDefaultTypeName() {
		return ShapefileConstants.DEFAULT_TYPE_NAME;
	}

	/**
	 * Get the type definition from a Shapefile.
	 * 
	 * @param source the Shapefile source
	 * @return the type definition or <code>null</code> in case reading the type
	 *         was not possible
	 */
	public static TypeDefinition readShapeType(LocatableInputSupplier<? extends InputStream> source) {
		ShapeSchemaReader reader = new ShapeSchemaReader();
		reader.setSource(source);
		try {
			reader.execute(null);
			Collection<? extends TypeDefinition> types = reader.getSchema()
					.getMappingRelevantTypes();
			if (!types.isEmpty()) {
				return types.iterator().next();
			}
			else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

}
