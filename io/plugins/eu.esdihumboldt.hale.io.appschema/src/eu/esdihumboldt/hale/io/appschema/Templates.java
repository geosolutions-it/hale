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

package eu.esdihumboldt.hale.io.appschema;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.google.common.io.Files;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class Templates {

	private static Templates instance;

	public static Templates getInstance() throws TemplateException {
		if (instance == null) {
			instance = new Templates();
		}

		return instance;
	}

	private final File tmpDir;
	private final VelocityEngine ve;

	private Templates() throws TemplateException {
		ve = new VelocityEngine();

		tmpDir = Files.createTempDir();
		tmpDir.deleteOnExit();

		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
				"org.apache.velocity.runtime.log.JdkLogChute");
		ve.setProperty(VelocityEngine.RESOURCE_LOADER, "file");
		ve.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader");
		ve.setProperty("file.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("file.resource.loader.path", tmpDir.getAbsolutePath());

		try {
			ve.init();
		} catch (Exception e) {
			throw new TemplateException(e);
		}
	}

	public InputStream loadTemplate(String templateResource, Map<String, Object> variables)
			throws TemplateException {
		VelocityContext vc = new VelocityContext(variables);

		try {
			File templateFile = copyTemplateToFile(templateResource);

			Template template = ve.getTemplate(templateFile.getName(), "UTF-8");

			StringWriter sw = new StringWriter();
			template.merge(vc, sw);

			templateFile.deleteOnExit();

			return new ByteArrayInputStream(sw.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			throw new TemplateException(e);
		}

	}

	private File copyTemplateToFile(String templateResource) throws IOException {
		InputStream templateStream = getClass().getResourceAsStream(templateResource);
		if (templateStream == null) {
			throw new IOException("Template resource not found: " + templateResource);
		}

		File templateFile = new File(tmpDir, createRandomTemplateName(templateResource));
		OutputStream outputStream = null;
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(templateFile));
			IOUtils.copy(templateStream, outputStream);

			return templateFile;
		} finally {
			closeAndIgnoreException(templateStream);
			closeAndIgnoreException(outputStream);
		}
	}

	private String createRandomTemplateName(String templateResource) {
		return "template" + templateResource.hashCode() + System.currentTimeMillis() + ".vm";
	}

	private void closeAndIgnoreException(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private void closeAndIgnoreException(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
