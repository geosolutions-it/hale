package eu.esdihumboldt.hale.io.appschema.ui;

import eu.esdihumboldt.hale.io.appschema.writer.AbstractAppSchemaConfigurator;
import eu.esdihumboldt.hale.ui.io.ExportWizard;

/**
 * 
 * TODO Type description
 * 
 * @author stefano
 */
public class AppSchemaAlignmentExportWizard extends ExportWizard<AbstractAppSchemaConfigurator> {

	public AppSchemaAlignmentExportWizard() {
		super(AbstractAppSchemaConfigurator.class);
	}

	@Override
	public void addPages() {
		// add the datastore configuration page
		addPage(new AppSchemaDataStoreConfigurationPage());
		super.addPages();
	}
}
