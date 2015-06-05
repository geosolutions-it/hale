package eu.esdihumboldt.hale.io.appschema.writer;

import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.DATASTORE_FILE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.DATASTORE_TEMPLATE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.FEATURETYPE_FILE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.FEATURETYPE_TEMPLATE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.LAYER_FILE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.LAYER_TEMPLATE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.NAMESPACE_FILE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.NAMESPACE_TEMPLATE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.WORKSPACE_FILE;
import static eu.esdihumboldt.hale.io.appschema.AppSchemaIO.WORKSPACE_TEMPLATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Platform;
import org.geotools.app_schema.AppSchemaDataAccessType;
import org.geotools.app_schema.NamespacesPropertyType.Namespace;
import org.geotools.app_schema.SourceDataStoresPropertyType.DataStore;
import org.geotools.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

import com.google.common.io.ByteStreams;

import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentWriter;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.io.appschema.AppSchemaIO;
import eu.esdihumboldt.hale.io.appschema.writer.internal.AppSchemaMappingWrapper;

public class AppSchemaAlignmentWriter extends AbstractAlignmentWriter {

	public static final String PARAMETER_DATASTORE = "datastore";

	private static final String DEFAULT_CONTENT_TYPE_ID = AppSchemaIO.CONTENT_TYPE_MAPPING;

	@Override
	public boolean isCancelable() {
		return false;
	}

	@Override
	protected IOReport execute(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		progress.begin("Translate HALE alignment to App-Schema Configuration",
				ProgressIndicator.UNKNOWN);
		if (getAlignment() == null) {
			throw new IOProviderConfigurationException("No alignment was provided.");
		}
		if (getTargetSchema() == null) {
			throw new IOProviderConfigurationException("No target schema was provided.");
		}
		if (getTarget() == null) {
			throw new IOProviderConfigurationException("No target was provided.");
		}

		if (getContentType() == null) {
			// contentType was not specified, use default (mapping file)
			setContentType(Platform.getContentTypeManager().getContentType(DEFAULT_CONTENT_TYPE_ID));
		}

		try {
			if (getContentType().getId().equals(AppSchemaIO.CONTENT_TYPE_MAPPING)) {
				writeMappingFile(reporter);
			}
			else if (getContentType().getId().equals(AppSchemaIO.CONTENT_TYPE_ARCHIVE)) {
				writeArchive(reporter);
			}
			else {
				throw new IOProviderConfigurationException("Unsupported content type: "
						+ getContentType().getName());
			}
		} catch (Exception e) {
			reporter.error(new IOMessageImpl(e.getMessage(), e));
			reporter.setSuccess(false);
			return reporter;
		}

		progress.end();
		reporter.setSuccess(true);
		return reporter;
	}

	protected void writeMappingFile(IOReporter reporter) throws Exception {
		DataStore dataStoreParam = getDataStoreParameter();
		AppSchemaMappingGenerator generator = new AppSchemaMappingGenerator(getAlignment(),
				getTargetSchema(), dataStoreParam);

		OutputStream out = getTarget().getOutput();
		generator.generateMapping(out, reporter);
		out.flush();
	}

	// TODO: refactor this method
	// TODO: close InputStreams after copying data
	protected void writeArchive(IOReporter reporter) throws Exception {
		DataStore dataStoreParam = getDataStoreParameter();
		AppSchemaMappingGenerator generator = new AppSchemaMappingGenerator(getAlignment(),
				getTargetSchema(), dataStoreParam);
		AppSchemaMappingWrapper wrapper = generator.generateMapping(reporter);
		AppSchemaDataAccessType mapping = wrapper.getAppSchemaMapping();

		// pick the target schemas from which interpolation variables will be
		// derived
		Schema selectedSchema = getTargetSchema().getSchemas().iterator().next();
		Map<String, String> variables = getInterpolationVariables(selectedSchema, wrapper);

		// save to archive
		final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(getTarget()
				.getOutput()));
		// add workspace folder
		ZipEntry workspaceFolder = new ZipEntry(variables.get("workspaceName") + "/");
		zip.putNextEntry(workspaceFolder);
		// add workspace file
		InputStream workspaceIS = AppSchemaIO.loadTemplate(WORKSPACE_TEMPLATE, variables);
		zip.putNextEntry(new ZipEntry(workspaceFolder.getName() + WORKSPACE_FILE));
		ByteStreams.copy(workspaceIS, zip);
		zip.closeEntry();
		// add namespace file
		InputStream namespaceIS = AppSchemaIO.loadTemplate(NAMESPACE_TEMPLATE, variables);
		zip.putNextEntry(new ZipEntry(workspaceFolder.getName() + NAMESPACE_FILE));
		ByteStreams.copy(namespaceIS, zip);
		zip.closeEntry();
		// add datastore folder
		ZipEntry dataStoreFolder = new ZipEntry(workspaceFolder.getName()
				+ variables.get("dataStoreName") + "/");
		zip.putNextEntry(dataStoreFolder);
		// add datastore file
		InputStream dataStoreIS = AppSchemaIO.loadTemplate(DATASTORE_TEMPLATE, variables);
		zip.putNextEntry(new ZipEntry(dataStoreFolder.getName() + DATASTORE_FILE));
		ByteStreams.copy(dataStoreIS, zip);
		zip.closeEntry();
		// add mapping file
		zip.putNextEntry(new ZipEntry(dataStoreFolder.getName() + variables.get("mappingFileName")));
		AppSchemaMappingGenerator.writeMappingConf(mapping, zip);
		zip.closeEntry();
		// add feature type entries
		for (FeatureTypeMapping ftMapping : mapping.getTypeMappings().getFeatureTypeMapping()) {
			String featureTypeName = stripPrefix(ftMapping.getTargetElement());
			String featureTypeId = featureTypeName + "_featureType";
			String layerName = featureTypeName;
			String layerId = layerName + "_layer";
			variables.put("featureTypeId", featureTypeId);
			variables.put("featureTypeName", featureTypeName);
			variables.put("layerId", layerId);
			variables.put("layerName", layerName);

			// add feature type folder
			ZipEntry featureTypeFolder = new ZipEntry(dataStoreFolder.getName() + featureTypeName
					+ "/");
			zip.putNextEntry(featureTypeFolder);
			// add feature type file
			InputStream featureTypeIS = AppSchemaIO.loadTemplate(FEATURETYPE_TEMPLATE, variables);
			zip.putNextEntry(new ZipEntry(featureTypeFolder.getName() + FEATURETYPE_FILE));
			ByteStreams.copy(featureTypeIS, zip);
			zip.closeEntry();
			// add layer file
			InputStream layerIS = AppSchemaIO.loadTemplate(LAYER_TEMPLATE, variables);
			zip.putNextEntry(new ZipEntry(featureTypeFolder.getName() + LAYER_FILE));
			ByteStreams.copy(layerIS, zip);
			zip.closeEntry();
		}

		// add secondary namespaces
		for (Namespace ns : mapping.getNamespaces().getNamespace()) {
			if (!ns.getUri().equals(selectedSchema.getNamespace())) {
				Map<String, String> secondaryVariables = getWorkspaceInterpolationVariables(ns);
				// add workspace folder
				ZipEntry secondaryWorkspaceFolder = new ZipEntry(ns.getPrefix() + "/");
				zip.putNextEntry(secondaryWorkspaceFolder);
				// add workspace file
				InputStream secondaryWorkspaceIS = AppSchemaIO.loadTemplate(WORKSPACE_TEMPLATE,
						secondaryVariables);
				zip.putNextEntry(new ZipEntry(secondaryWorkspaceFolder.getName() + WORKSPACE_FILE));
				ByteStreams.copy(secondaryWorkspaceIS, zip);
				zip.closeEntry();
				// add namespace file
				InputStream secondaryNamespaceIS = AppSchemaIO.loadTemplate(NAMESPACE_TEMPLATE,
						secondaryVariables);
				zip.putNextEntry(new ZipEntry(secondaryWorkspaceFolder.getName() + NAMESPACE_FILE));
				ByteStreams.copy(secondaryNamespaceIS, zip);
				zip.closeEntry();
			}
		}

		zip.close();
	}

	private String extractSchemaName(URI schemaLocation) {
		String path = schemaLocation.getPath();
		String fragment = schemaLocation.getFragment();
		if (fragment != null && !fragment.isEmpty()) {
			path = path.replace(fragment, "");
		}
		int lastSlashIdx = path.lastIndexOf('/');
		int lastDotIdx = path.lastIndexOf('.');
		if (lastSlashIdx >= 0) {
			if (lastDotIdx >= 0) {
				return path.substring(lastSlashIdx + 1, lastDotIdx);
			}
			else {
				// no dot
				return path.substring(lastSlashIdx + 1);
			}
		}
		else {
			// no slash, no dot
			return path;
		}
	}

	private Map<String, String> getInterpolationVariables(Schema selectedSchema,
			AppSchemaMappingWrapper wrapper) {
		Namespace ns = wrapper.getOrCreateNamespace(selectedSchema.getNamespace(), null);
		Map<String, String> variables = getWorkspaceInterpolationVariables(ns);

		String dataStoreName = extractSchemaName(selectedSchema.getLocation());
		String dataStoreId = dataStoreName + "_datastore";
		String mappingFileName = dataStoreName + ".xml";

		variables.put("dataStoreId", dataStoreId);
		variables.put("dataStoreName", dataStoreName);
		variables.put("mappingFileName", mappingFileName);

		return variables;
	}

	private Map<String, String> getWorkspaceInterpolationVariables(Namespace ns) {
		String prefix = ns.getPrefix();
		String uri = ns.getUri();
		String workspaceId = prefix + "_workspace";
		String workspaceName = prefix;
		String namespaceId = prefix + "_namespace";

		Map<String, String> variables = new HashMap<String, String>();
		variables.put("prefix", prefix);
		variables.put("uri", uri);
		variables.put("workspaceId", workspaceId);
		variables.put("workspaceName", workspaceName);
		variables.put("namespaceId", namespaceId);

		return variables;
	}

	private String stripPrefix(String qualifiedName) {
		if (qualifiedName == null) {
			return null;
		}

		String[] prefixAndName = qualifiedName.split(":");
		if (prefixAndName.length == 2) {
			return prefixAndName[1];
		}
		else {
			return null;
		}
	}

	@Override
	protected String getDefaultTypeName() {
		return "GeoTools app-schema";
	}

	protected DataStore getDataStoreParameter() {
		return getParameter(PARAMETER_DATASTORE).as(DataStore.class);
	}

}
