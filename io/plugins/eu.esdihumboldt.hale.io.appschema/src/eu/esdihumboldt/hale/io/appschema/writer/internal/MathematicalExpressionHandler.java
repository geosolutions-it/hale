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

import static eu.esdihumboldt.cst.functions.numeric.MathematicalExpressionFunction.PARAMETER_EXPRESSION;

/**
 * TODO Type description
 * 
 * @author stefano
 */
public class MathematicalExpressionHandler extends AbstractPropertyTransformationHandler {

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler#getSourceExpressionAsCQL()
	 */
	@Override
	protected String getSourceExpressionAsCQL() {
		// TODO: verify math expressions work as-is in CQL
		String mathExpression = propertyCell.getTransformationParameters()
				.get(PARAMETER_EXPRESSION).get(0).getStringRepresentation();

		return mathExpression;
	}
}
