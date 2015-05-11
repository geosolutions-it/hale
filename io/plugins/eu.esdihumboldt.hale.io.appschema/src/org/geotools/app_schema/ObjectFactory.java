//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.05.06 at 05:40:56 PM CEST 
//


package org.geotools.app_schema;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.geotools.app_schema package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AppSchemaDataAccess_QNAME = new QName("http://www.geotools.org/app-schema", "AppSchemaDataAccess");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.geotools.app_schema
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TargetTypesPropertyType.FeatureType }
     * 
     */
    public TargetTypesPropertyType.FeatureType createTargetTypesPropertyTypeFeatureType() {
        return new TargetTypesPropertyType.FeatureType();
    }

    /**
     * Create an instance of {@link AttributeMappingType.ClientProperty }
     * 
     */
    public AttributeMappingType.ClientProperty createAttributeMappingTypeClientProperty() {
        return new AttributeMappingType.ClientProperty();
    }

    /**
     * Create an instance of {@link IncludesPropertyType }
     * 
     */
    public IncludesPropertyType createIncludesPropertyType() {
        return new IncludesPropertyType();
    }

    /**
     * Create an instance of {@link SourceDataStoresPropertyType.DataStore.Parameters.Parameter }
     * 
     */
    public SourceDataStoresPropertyType.DataStore.Parameters.Parameter createSourceDataStoresPropertyTypeDataStoreParametersParameter() {
        return new SourceDataStoresPropertyType.DataStore.Parameters.Parameter();
    }

    /**
     * Create an instance of {@link AttributeExpressionMappingType.Expression }
     * 
     */
    public AttributeExpressionMappingType.Expression createAttributeExpressionMappingTypeExpression() {
        return new AttributeExpressionMappingType.Expression();
    }

    /**
     * Create an instance of {@link AttributeExpressionMappingType }
     * 
     */
    public AttributeExpressionMappingType createAttributeExpressionMappingType() {
        return new AttributeExpressionMappingType();
    }

    /**
     * Create an instance of {@link SourceDataStoresPropertyType.DataStore.Parameters }
     * 
     */
    public SourceDataStoresPropertyType.DataStore.Parameters createSourceDataStoresPropertyTypeDataStoreParameters() {
        return new SourceDataStoresPropertyType.DataStore.Parameters();
    }

    /**
     * Create an instance of {@link TypeMappingsPropertyType }
     * 
     */
    public TypeMappingsPropertyType createTypeMappingsPropertyType() {
        return new TypeMappingsPropertyType();
    }

    /**
     * Create an instance of {@link AppSchemaDataAccessType }
     * 
     */
    public AppSchemaDataAccessType createAppSchemaDataAccessType() {
        return new AppSchemaDataAccessType();
    }

    /**
     * Create an instance of {@link NamespacesPropertyType }
     * 
     */
    public NamespacesPropertyType createNamespacesPropertyType() {
        return new NamespacesPropertyType();
    }

    /**
     * Create an instance of {@link TypeMappingsPropertyType.FeatureTypeMapping }
     * 
     */
    public TypeMappingsPropertyType.FeatureTypeMapping createTypeMappingsPropertyTypeFeatureTypeMapping() {
        return new TypeMappingsPropertyType.FeatureTypeMapping();
    }

    /**
     * Create an instance of {@link SourceDataStoresPropertyType.DataStore }
     * 
     */
    public SourceDataStoresPropertyType.DataStore createSourceDataStoresPropertyTypeDataStore() {
        return new SourceDataStoresPropertyType.DataStore();
    }

    /**
     * Create an instance of {@link AttributeMappingType }
     * 
     */
    public AttributeMappingType createAttributeMappingType() {
        return new AttributeMappingType();
    }

    /**
     * Create an instance of {@link TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings }
     * 
     */
    public TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings createTypeMappingsPropertyTypeFeatureTypeMappingAttributeMappings() {
        return new TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings();
    }

    /**
     * Create an instance of {@link NamespacesPropertyType.Namespace }
     * 
     */
    public NamespacesPropertyType.Namespace createNamespacesPropertyTypeNamespace() {
        return new NamespacesPropertyType.Namespace();
    }

    /**
     * Create an instance of {@link SourceDataStoresPropertyType }
     * 
     */
    public SourceDataStoresPropertyType createSourceDataStoresPropertyType() {
        return new SourceDataStoresPropertyType();
    }

    /**
     * Create an instance of {@link TargetTypesPropertyType }
     * 
     */
    public TargetTypesPropertyType createTargetTypesPropertyType() {
        return new TargetTypesPropertyType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AppSchemaDataAccessType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.geotools.org/app-schema", name = "AppSchemaDataAccess")
    public JAXBElement<AppSchemaDataAccessType> createAppSchemaDataAccess(AppSchemaDataAccessType value) {
        return new JAXBElement<AppSchemaDataAccessType>(_AppSchemaDataAccess_QNAME, AppSchemaDataAccessType.class, null, value);
    }

}
