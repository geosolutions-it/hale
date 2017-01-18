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

package eu.esdihumboldt.hale.io.gml.reader.internal;

import java.io.IOException;
import java.io.InputStream;

import eu.esdihumboldt.hale.common.core.io.IOProvider;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOProvider;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader;
import eu.esdihumboldt.hale.common.instance.io.impl.AbstractInstanceReader;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection;
import eu.esdihumboldt.hale.io.gml.reader.internal.wfs.WfsBackedGmlInstanceCollection;

/**
 * Reads XML/GML from a stream
 * 
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
public class StreamGmlReader extends AbstractInstanceReader {

	/**
	 * The name of the parameter specifying if the root element should be
	 * ignored and thus not be loaded as an instance. Parameter value defaults
	 * to <code>true</code>.
	 */
	public static final String PARAM_IGNORE_ROOT = "ignoreRoot";

	/**
	 * The name of the parameter specifying if parsing of the XML instances
	 * should happen strictly according to the schema or if also invalid
	 * property paths will be allowed. Parameter value defaults to
	 * <code>false</code>.
	 */
	public static final String PARAM_STRICT = "strict";

	/**
	 * The name of the parameter specifying if parsing of the XML instances
	 * should allow types and properties with namespaces that differ from those
	 * defined in the schema. Parameter value defaults to <code>false</code>.
	 */
	public static final String PARAM_IGNORE_NAMESPACES = "ignoreNamespaces";

	/**
	 * The name of the parameter specifying the maximum number of features to
	 * retrieve per single WFS GetFeature request. Only useful if source is a
	 * WFS GetFeature request URI.
	 */
	public static final String PARAM_FEATURES_PER_WFS_REQUEST = "featuresPerWfsRequest";

	private InstanceCollection instances;

	private final boolean restrictToFeatures;

	/**
	 * Constructor
	 * 
	 * @param restrictToFeatures if only instances that are GML features shall
	 *            be loaded
	 */
	public StreamGmlReader(boolean restrictToFeatures) {
		super();
		this.restrictToFeatures = restrictToFeatures;

		addSupportedParameter(PARAM_IGNORE_ROOT);
		addSupportedParameter(PARAM_STRICT);
		addSupportedParameter(PARAM_IGNORE_NAMESPACES);
		addSupportedParameter(PARAM_FEATURES_PER_WFS_REQUEST);
	}

	/**
	 * @see AbstractIOProvider#execute(ProgressIndicator, IOReporter)
	 */
	@Override
	protected IOReport execute(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		progress.begin("Prepare loading of " + getTypeName(), ProgressIndicator.UNKNOWN);

		try {
			boolean ignoreRoot = getParameter(PARAM_IGNORE_ROOT).as(Boolean.class, true);
			boolean strict = getParameter(PARAM_STRICT).as(Boolean.class, false);
			boolean ignoreNamespaces = getParameter(PARAM_IGNORE_NAMESPACES).as(Boolean.class,
					false);
			int featuresPerRequest = getParameter(PARAM_FEATURES_PER_WFS_REQUEST).as(Integer.class,
					1000);

			LocatableInputSupplier<? extends InputStream> source = getSource();
			if (source.getLocation() != null) {
				String scheme = source.getLocation().getScheme();
				String query = source.getLocation().getQuery();

				if (query != null && scheme != null
						&& query.toLowerCase().contains("request=getfeature")
						&& (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {

					// check if WFS is reachable and responds?

					instances = new WfsBackedGmlInstanceCollection(getSource(), getSourceSchema(),
							restrictToFeatures, ignoreRoot, strict, ignoreNamespaces,
							getCrsProvider(), this, featuresPerRequest);
				}
			}
			else {
				instances = new GmlInstanceCollection(getSource(), getSourceSchema(),
						restrictToFeatures, ignoreRoot, strict, ignoreNamespaces, getCrsProvider(),
						this);
			}

			// TODO any kind of analysis on file? e.g. types and size - would
			// also give feedback to the user if the file can be loaded
			reporter.setSuccess(true);
		} catch (Throwable e) {
			reporter.setSuccess(false);
		}

		return reporter;
	}

	/**
	 * @see InstanceReader#getInstances()
	 */
	@Override
	public InstanceCollection getInstances() {
		return instances;
	}

	/**
	 * @see AbstractIOProvider#getDefaultTypeName()
	 */
	@Override
	protected String getDefaultTypeName() {
		return "GML";
	}

	/**
	 * @see IOProvider#isCancelable()
	 */
	@Override
	public boolean isCancelable() {
		// FIXME for now not
		return false;
	}

}
