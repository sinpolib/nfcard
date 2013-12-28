/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.sinpo.xnfc.nfc.bean;

import java.util.ArrayList;

import com.sinpo.xnfc.SPEC;

public class Card extends Application {
	public static final Card EMPTY = new Card();

	private final ArrayList<Application> applications;

	public Card() {
		applications = new ArrayList<Application>(2);
	}

	public Exception getReadingException() {
		return (Exception) getProperty(SPEC.PROP.EXCEPTION);
	}

	public boolean hasReadingException() {
		return hasProperty(SPEC.PROP.EXCEPTION);
	}

	public final boolean isUnknownCard() {
		return applicationCount() == 0;
	}

	public final int applicationCount() {
		return applications.size();
	}

	public final Application getApplication(int index) {
		return applications.get(index);
	}

	public final void addApplication(Application app) {
		if (app != null)
			applications.add(app);
	}

	public String toHtml() {
		return HtmlFormatter.formatCardInfo(this);
	}
}
