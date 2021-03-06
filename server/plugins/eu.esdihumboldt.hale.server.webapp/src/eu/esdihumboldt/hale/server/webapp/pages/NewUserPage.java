/*
 * Copyright (c) 2013 Data Harmonisation Panel
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

package eu.esdihumboldt.hale.server.webapp.pages;

import org.apache.wicket.markup.head.IHeaderResponse;

import eu.esdihumboldt.hale.server.webapp.components.user.UserDetailsForm;

/**
 * Page to welcome new users and ask them for their name, email etc.
 * 
 * @author Simon Templer
 */
public class NewUserPage extends SecuredPage {

	private static final long serialVersionUID = 5219827119661291677L;

	@Override
	protected void addControls() {
		super.addControls();

		add(new UserDetailsForm("details-form", true));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		// add boxes css to page
		response.render(PagesCSS.boxesPage());
	}

}
