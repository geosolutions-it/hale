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

import java.net.URI;

import eu.esdihumboldt.cst.test.TransformationExampleImpl;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class LandCoverTransformation extends TransformationExampleImpl {

	/**
	 * @param sourceSchemaLocation
	 * @param targetSchemaLocation
	 * @param alignmentLocation
	 * @param sourceDataLocation
	 * @param targetDataLocation
	 * @param targetContainerNamespace
	 * @param targetContainerName
	 */
	public LandCoverTransformation(URI sourceSchemaLocation, URI targetSchemaLocation,
			URI alignmentLocation, URI sourceDataLocation, URI targetDataLocation,
			String targetContainerNamespace, String targetContainerName) {
		super(sourceSchemaLocation, targetSchemaLocation, alignmentLocation, sourceDataLocation,
				targetDataLocation, targetContainerNamespace, targetContainerName);
		// TODO Auto-generated constructor stub
	}

}
