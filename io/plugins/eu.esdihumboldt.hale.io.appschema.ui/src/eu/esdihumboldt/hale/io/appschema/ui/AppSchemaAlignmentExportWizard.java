package eu.esdihumboldt.hale.io.appschema.ui;

import eu.esdihumboldt.hale.io.appschema.writer.AppSchemaAlignmentWriter;
import eu.esdihumboldt.hale.ui.io.ExportWizard;

/**
 * 
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaAlignmentExportWizard extends ExportWizard<AppSchemaAlignmentWriter> {

	public AppSchemaAlignmentExportWizard() {
		super(AppSchemaAlignmentWriter.class);
	}

	@Override
	public void addPages() {
		// add the datastore configuration page
		addPage(new AppSchemaDataStoreConfigurationPage());
		super.addPages();
	}
}
