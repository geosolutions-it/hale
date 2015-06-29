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

import org.geotools.app_schema.AttributeMappingType;

import eu.esdihumboldt.hale.common.align.model.Cell;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public interface PropertyTransformationHandler {

	public AttributeMappingType handlePropertyTransformation(Cell propertyCell,
			AppSchemaMappingContext context);

}
