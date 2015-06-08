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

package eu.esdihumboldt.hale.io.appschema.compatibility;

import com.google.common.base.Predicate;
import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.compatibility.CompatibilityMode;
import eu.esdihumboldt.hale.common.align.compatibility.CompatibilityModeUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.filter.AbstractGeotoolsFilter;
import eu.esdihumboldt.hale.common.instance.model.Filter;
import eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandlerFactory;
import eu.esdihumboldt.hale.io.appschema.writer.internal.TypeTransformationHandlerFactory;
import eu.esdihumboldt.hale.io.appschema.writer.internal.UnsupportedTransformationException;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class GeoServerCompatibilityMode implements CompatibilityMode {

	/**
	 * @see eu.esdihumboldt.hale.common.align.compatibility.CompatibilityMode#supportsFunction(java.lang.String)
	 */
	@Override
	public boolean supportsFunction(String id) {
		return (checkTypeFunction(id) || checkPropertyFunction(id));
	}

	/**
	 * @see eu.esdihumboldt.hale.common.align.compatibility.CompatibilityMode#supportsCell(eu.esdihumboldt.hale.common.align.model.Cell)
	 */
	@Override
	public boolean supportsCell(Cell cell) {
		// type filters are not supported
		if (!checkNoTypeFilters(cell)) {
			return false;
		}

		// only accept cells with supported (property) filters
		if (!CompatibilityModeUtil.checkFilters(cell, new Predicate<Filter>() {

			@Override
			public boolean apply(Filter filter) {
				/*
				 * XXX not nice to check it like this, but will do for now
				 */
				return filter instanceof AbstractGeotoolsFilter;
			}
		})) {
			return false;
		}

		return true;
	}

	private boolean checkTypeFunction(String id) {
		try {
			TypeTransformationHandlerFactory.getInstance().createTypeTransformationHandler(id);
		} catch (UnsupportedTransformationException e) {
			return false;
		}

		return true;
	}

	private boolean checkPropertyFunction(String id) {
		try {
			PropertyTransformationHandlerFactory.getInstance().createPropertyTransformationHandler(
					id);
		} catch (UnsupportedTransformationException e) {
			return false;
		}

		return true;
	}

	private boolean checkNoTypeFilters(Cell cell) {
		ListMultimap<String, ? extends Entity> sourceEntities = cell.getSource();

		if (sourceEntities == null) {
			return true;
		}
		else {
			for (Entity source : sourceEntities.values()) {
				if (source.getDefinition().getFilter() != null) {
					return false;
				}
			}
		}

		return true;
	}
}
