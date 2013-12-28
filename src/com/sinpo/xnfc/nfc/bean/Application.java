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

import com.sinpo.xnfc.SPEC;

import android.util.SparseArray;

public class Application {
	private final SparseArray<Object> properties = new SparseArray<Object>();

	public final void setProperty(SPEC.PROP prop, Object value) {
		properties.put(prop.ordinal(), value);
	}

	public final Object getProperty(SPEC.PROP prop) {
		return properties.get(prop.ordinal());
	}

	public final boolean hasProperty(SPEC.PROP prop) {
		return getProperty(prop) != null;
	}

	public final String getStringProperty(SPEC.PROP prop) {
		final Object v = getProperty(prop);
		return (v != null) ? v.toString() : "";
	}

	public final float getFloatProperty(SPEC.PROP prop) {
		final Object v = getProperty(prop);

		if (v == null)
			return Float.NaN;

		if (v instanceof Float)
			return ((Float) v).floatValue();

		return Float.parseFloat(v.toString());
	}
}
