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

import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class PropertyTransformationHandlerFactory {

	private static PropertyTransformationHandlerFactory instance;

	private PropertyTransformationHandlerFactory() {

	}

	public static PropertyTransformationHandlerFactory getInstance() {
		if (instance == null) {
			instance = new PropertyTransformationHandlerFactory();
		}

		return instance;
	}

	public PropertyTransformationHandler createPropertyTransformationHandler(
			String propertyTransformationIdentifier) throws UnsupportedTransformationException {
		if (propertyTransformationIdentifier == null
				|| propertyTransformationIdentifier.trim().isEmpty()) {
			throw new IllegalArgumentException("propertyTransformationIdentifier must be set");
		}

		if (propertyTransformationIdentifier.equals(RenameFunction.ID)) {
			return new RenameHandler();
		}
		else {
			String errMsg = String.format("Unsupported property transformation %s",
					propertyTransformationIdentifier);
			throw new UnsupportedTransformationException(errMsg, propertyTransformationIdentifier);
		}
	}
}
