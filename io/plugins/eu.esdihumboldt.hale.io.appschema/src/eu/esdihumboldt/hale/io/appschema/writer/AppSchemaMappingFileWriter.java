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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Platform;

import com.google.common.io.ByteStreams;

import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.io.appschema.AppSchemaIO;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaMappingFileWriter extends AbstractAppSchemaConfigurator {

	private static final String DEFAULT_CONTENT_TYPE_ID = AppSchemaIO.CONTENT_TYPE_MAPPING;

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.AbstractAppSchemaConfigurator#handleMapping(eu.esdihumboldt.hale.common.core.io.ProgressIndicator,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	protected void handleMapping(AppSchemaMappingGenerator generator, ProgressIndicator progress,
			IOReporter reporter) throws IOProviderConfigurationException, IOException {
		if (getContentType() == null) {
			// contentType was not specified, use default (mapping file)
			setContentType(Platform.getContentTypeManager().getContentType(DEFAULT_CONTENT_TYPE_ID));
		}

		if (getContentType().getId().equals(AppSchemaIO.CONTENT_TYPE_MAPPING)) {
			writeMappingFile(generator, reporter);
		}
		else if (getContentType().getId().equals(AppSchemaIO.CONTENT_TYPE_ARCHIVE)) {
			writeArchive(generator, reporter);
		}
		else {
			throw new IOProviderConfigurationException("Unsupported content type: "
					+ getContentType().getName());
		}
	}

	private void writeMappingFile(AppSchemaMappingGenerator generator, IOReporter reporter)
			throws IOException {
		OutputStream out = getTarget().getOutput();
		generator.generateMapping(out, reporter);
		out.flush();
	}

	// TODO: refactor this method
	// TODO: close InputStreams after copying data
	private void writeArchive(AppSchemaMappingGenerator generator, IOReporter reporter)
			throws IOException {
		Map<String, String> variables = generator.getAppSchemaDataStore();

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
		generator.writeMappingConf(zip);
		zip.closeEntry();

		// add feature type entries
		Map<String, Map<String, String>> featureTypes = generator.getFeatureTypes();
		for (String featureTypeName : featureTypes.keySet()) {
			variables.putAll(featureTypes.get(featureTypeName));

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
		Map<String, Map<String, String>> secondaryNamespaces = generator.getSecondaryNamespaces();
		for (String nsPrefix : secondaryNamespaces.keySet()) {
			Map<String, String> secondaryVariables = secondaryNamespaces.get(nsPrefix);
			// add workspace folder
			ZipEntry secondaryWorkspaceFolder = new ZipEntry(nsPrefix + "/");
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

		zip.close();
	}

}
