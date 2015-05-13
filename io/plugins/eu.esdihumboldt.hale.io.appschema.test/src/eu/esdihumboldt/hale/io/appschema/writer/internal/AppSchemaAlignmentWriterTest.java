package eu.esdihumboldt.hale.io.appschema.writer.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.io.AlignmentIO;
import eu.esdihumboldt.hale.common.align.io.AlignmentReader;
import eu.esdihumboldt.hale.common.align.io.impl.JaxbAlignmentReader;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.core.io.IOProvider;
import eu.esdihumboldt.hale.common.core.io.extension.IOProviderDescriptor;
import eu.esdihumboldt.hale.common.core.io.extension.IOProviderExtension;
import eu.esdihumboldt.hale.common.core.io.impl.LogProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfo;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfoAware;
import eu.esdihumboldt.hale.common.core.io.project.impl.ArchiveProjectReader;
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.io.project.model.ProjectFileInfo;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace;
import eu.esdihumboldt.util.io.PathUpdate;

public class AppSchemaAlignmentWriterTest {

	private static final ALogger log = ALoggerFactory.getLogger(AppSchemaAlignmentWriterTest.class);

	private static final String PROJECT_LOCATION = "/data/landcover.halez";

	private static DefaultSchemaSpace sourceSchemaSpace;
	private static DefaultSchemaSpace targetSchemaSpace;

	private static Project project;
	private static Alignment alignment;
	private static File tempDir;

	@BeforeClass
	public static void loadTestProject() {

		try {
			URL archiveLocation = AppSchemaAlignmentWriterTest.class.getResource(PROJECT_LOCATION);

			ArchiveProjectReader projectReader = new ArchiveProjectReader();
			projectReader.setSource(new DefaultInputSupplier(archiveLocation.toURI()));
			IOReport report = projectReader.execute(new LogProgressIndicator());
			if (!report.isSuccess()) {
				throw new RuntimeException("project reader execution failed");
			}
			tempDir = projectReader.getTemporaryFiles().iterator().next();

			project = projectReader.getProject();
			assertNotNull(project);

			sourceSchemaSpace = new DefaultSchemaSpace();
			targetSchemaSpace = new DefaultSchemaSpace();

			// load schemas
			List<IOConfiguration> resources = project.getResources();
			for (IOConfiguration resource : resources) {
				String actionId = resource.getActionId();
				String providerId = resource.getProviderId();

				// get provider
				IOProvider provider = null;
				IOProviderDescriptor descriptor = IOProviderExtension.getInstance().getFactory(
						providerId);
				if (descriptor == null) {
					throw new RuntimeException("Could not load I/O provider with ID: "
							+ resource.getProviderId());
				}

				provider = descriptor.createExtensionObject();
				provider.loadConfiguration(resource.getProviderConfiguration());
				prepareProvider(provider, project, tempDir.toURI());

				IOReport providerReport = provider.execute(new LogProgressIndicator());
				if (!providerReport.isSuccess()) {
					throw new RuntimeException("I/O provider execution failed");
				}

				// handle results
				// TODO: could (should?) be done by an advisor
				if (provider instanceof SchemaReader) {
					Schema schema = ((SchemaReader) provider).getSchema();
					if (actionId.equals(SchemaIO.ACTION_LOAD_SOURCE_SCHEMA)) {
						sourceSchemaSpace.addSchema(schema);
					}
					else if (actionId.equals(SchemaIO.ACTION_LOAD_TARGET_SCHEMA)) {
						targetSchemaSpace.addSchema(schema);
					}
				}
			}

			// load alignment
			List<ProjectFileInfo> projectFiles = project.getProjectFiles();
			for (ProjectFileInfo projectFile : projectFiles) {
				if (projectFile.getName().equals(AlignmentIO.PROJECT_FILE_ALIGNMENT)) {
					AlignmentReader alignReader = new JaxbAlignmentReader();
					alignReader.setSource(new DefaultInputSupplier(projectFile.getLocation()));
					alignReader.setSourceSchema(sourceSchemaSpace);
					alignReader.setTargetSchema(targetSchemaSpace);
					alignReader.setPathUpdater(new PathUpdate(null, null));
					IOReport alignReport = alignReader.execute(new LogProgressIndicator());
					if (!alignReport.isSuccess()) {
						throw new RuntimeException("alignment reader execution failed");
					}

					alignment = alignReader.getAlignment();
					assertNotNull(alignment);

					break;
				}
			}
		} catch (Exception e) {
			log.error("Exception occurred", e);
			fail("Test project could not be loaded: " + e.getMessage());
		}

	}

	// adapted from DefaultIOAdvisor and subclasses
	private static void prepareProvider(IOProvider provider, ProjectInfo projectInfo,
			URI projectLocation) {
		if (provider instanceof ProjectInfoAware) {
			ProjectInfoAware pia = (ProjectInfoAware) provider;
			pia.setProjectInfo(projectInfo);
			pia.setProjectLocation(projectLocation);
		}
		if (provider instanceof InstanceReader) {
			InstanceReader ir = (InstanceReader) provider;
			ir.setSourceSchema(sourceSchemaSpace);
		}
	}

	@AfterClass
	public static void cleanUp() throws IOException {
		if (tempDir != null && tempDir.exists()) {
			FileUtils.deleteDirectory(tempDir);
		}
	}

	@Test
	public void testProject() {
		assertNotNull(project);

//		List<ProjectFileInfo> projectFiles = project.getProjectFiles();
//		for (ProjectFileInfo projectFile : projectFiles) {
//			log.info("file name: " + projectFile.getName());
//			log.info("file location: " + projectFile.getLocation().toString());
//		}
//		List<IOConfiguration> resources = project.getResources();
//		for (IOConfiguration resource : resources) {
//			log.info("resource actionId: " + resource.getActionId());
//			log.info("resource providerId: " + resource.getProviderId());
//		}

		assertNotNull(alignment);
		assertEquals(2, alignment.getTypeCells().size());
	}

	@Test
	public void testMapping() throws Exception {

		final File targetFile = File.createTempFile(Long.toString(System.currentTimeMillis()),
				".xml");
		// TODO: uncomment next line in production
//		targetFile.deleteOnExit();

//		ServiceProvider serviceProvider = new ServiceManager(PROJECT_LOCATION);

		AppSchemaAlignmentWriter alignWriter = new AppSchemaAlignmentWriter();
		prepareProvider(alignWriter, project, tempDir.toURI());
		alignWriter.setAlignment(alignment);
		alignWriter.setSourceSchema(sourceSchemaSpace);
		alignWriter.setTargetSchema(targetSchemaSpace);
		alignWriter.setTarget(new FileIOSupplier(targetFile));
//		alignWriter.setServiceProvider(serviceProvider);

		IOReport report = alignWriter.execute(new LogProgressIndicator());
		assertNotNull(report);
		assertTrue(report.isSuccess());

	}
}
