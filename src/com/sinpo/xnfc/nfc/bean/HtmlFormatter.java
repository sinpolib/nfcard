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

import java.util.Collection;

import com.sinpo.xnfc.SPEC;

public final class HtmlFormatter {
	static String formatCardInfo(Card card) {

		final StringBuilder ret = new StringBuilder();

		startTag(ret, SPEC.TAG_BLK);

		Collection<Application> apps = card.getApplications();

		boolean first = true;
		for (Application app : apps) {

			if (first) {
				first = false;
			} else {
				newline(ret);
				newline(ret);
			}

			formatApplicationInfo(ret, app);
		}

		endTag(ret, SPEC.TAG_BLK);

		return ret.toString();
	}

	private static void startTag(StringBuilder out, String tag) {
		out.append('<').append(tag).append('>');
	}

	private static void endTag(StringBuilder out, String tag) {
		out.append('<').append('/').append(tag).append('>');
	}

	private static void newline(StringBuilder out) {
		out.append("<br />");
	}

	private static void spliter(StringBuilder out) {
		out.append("\n<").append(SPEC.TAG_SP).append(" />\n");
	}

	private static boolean formatProperty(StringBuilder out, String tag, Object value) {
		if (value == null)
			return false;

		startTag(out, tag);
		out.append(value.toString());
		endTag(out, tag);

		return true;
	}

	private static boolean formatProperty(StringBuilder out, String tag, Object prop, String value) {
		if (value == null || value.isEmpty())
			return false;

		startTag(out, tag);
		out.append(prop.toString());
		endTag(out, tag);

		startTag(out, SPEC.TAG_TEXT);
		out.append(value);
		endTag(out, SPEC.TAG_TEXT);

		return true;
	}

	private static boolean formatApplicationInfo(StringBuilder out, Application app) {

		if (!formatProperty(out, SPEC.TAG_H1, app.getProperty(SPEC.PROP.ID)))
			return false;

		newline(out);
		spliter(out);
		newline(out);

		{
			SPEC.PROP prop = SPEC.PROP.SERIAL;
			if (formatProperty(out, SPEC.TAG_LAB, prop, app.getStringProperty(prop)))
				newline(out);
		}

		{
			SPEC.PROP prop = SPEC.PROP.PARAM;
			if (formatProperty(out, SPEC.TAG_LAB, prop, app.getStringProperty(prop)))
				newline(out);
		}

		{
			SPEC.PROP prop = SPEC.PROP.VERSION;
			if (formatProperty(out, SPEC.TAG_LAB, prop, app.getStringProperty(prop)))
				newline(out);
		}

		{
			SPEC.PROP prop = SPEC.PROP.DATE;
			if (formatProperty(out, SPEC.TAG_LAB, prop, app.getStringProperty(prop)))
				newline(out);
		}

		{
			SPEC.PROP prop = SPEC.PROP.COUNT;
			if (formatProperty(out, SPEC.TAG_LAB, prop, app.getStringProperty(prop)))
				newline(out);
		}

		{
			SPEC.PROP prop = SPEC.PROP.TLIMIT;
			Float balance = (Float) app.getProperty(prop);
			if (balance != null && !balance.isNaN()) {
				String cur = app.getProperty(SPEC.PROP.CURRENCY).toString();
				String val = String.format("%.2f %s", balance, cur);
				if (formatProperty(out, SPEC.TAG_LAB, prop, val))
					newline(out);
			}
		}

		{
			SPEC.PROP prop = SPEC.PROP.DLIMIT;
			Float balance = (Float) app.getProperty(prop);
			if (balance != null && !balance.isNaN()) {
				String cur = app.getProperty(SPEC.PROP.CURRENCY).toString();
				String val = String.format("%.2f %s", balance, cur);
				if (formatProperty(out, SPEC.TAG_LAB, prop, val))
					newline(out);
			}
		}

		{
			SPEC.PROP prop = SPEC.PROP.ECASH;
			Float balance = (Float) app.getProperty(prop);

			if (balance != null) {
				formatProperty(out, SPEC.TAG_LAB, prop);
				if (balance.isNaN()) {
					out.append(SPEC.PROP.ACCESS);
				} else {
					formatProperty(out, SPEC.TAG_H2, String.format("%.2f ", balance));
					formatProperty(out, SPEC.TAG_LAB, app.getProperty(SPEC.PROP.CURRENCY)
							.toString());
				}
				newline(out);
			}
		}

		{
			SPEC.PROP prop = SPEC.PROP.BALANCE;
			Float balance = (Float) app.getProperty(prop);

			if (balance != null) {
				formatProperty(out, SPEC.TAG_LAB, prop);
				if (balance.isNaN()) {
					out.append(SPEC.PROP.ACCESS);
				} else {
					formatProperty(out, SPEC.TAG_H2, String.format("%.2f ", balance));
					formatProperty(out, SPEC.TAG_LAB, app.getProperty(SPEC.PROP.CURRENCY)
							.toString());
				}
				newline(out);
			}
		}

		{
			SPEC.PROP prop = SPEC.PROP.OLIMIT;
			Float balance = (Float) app.getProperty(prop);
			if (balance != null && !balance.isNaN()) {
				String cur = app.getProperty(SPEC.PROP.CURRENCY).toString();
				String val = String.format("%.2f %s", balance, cur);
				if (formatProperty(out, SPEC.TAG_LAB, prop, val))
					newline(out);
			}
		}

		{
			SPEC.PROP prop = SPEC.PROP.TRANSLOG;
			String[] logs = (String[]) app.getProperty(prop);
			if (logs != null && logs.length > 0) {

				spliter(out);
				newline(out);

				startTag(out, SPEC.TAG_PARAG);

				formatProperty(out, SPEC.TAG_LAB, prop);
				newline(out);

				endTag(out, SPEC.TAG_PARAG);

				for (String log : logs) {
					formatProperty(out, SPEC.TAG_H3, log);
					newline(out);
				}

				newline(out);
			}
		}

		return true;
	}
}
