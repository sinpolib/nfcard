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

package com.sinpo.xnfc.nfc.reader.pboc;

import com.sinpo.xnfc.SPEC;
import com.sinpo.xnfc.nfc.Util;
import com.sinpo.xnfc.nfc.bean.Application;
import com.sinpo.xnfc.nfc.tech.Iso7816;

import android.annotation.SuppressLint;

final class CityUnion extends StandardPboc {
	private Object applicationId = SPEC.APP.UNKNOWN;

	@Override
	protected Object getApplicationId() {
		return applicationId;
	}

	@Override
	protected byte[] getMainApplicationId() {
		return new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03,
				(byte) 0x86, (byte) 0x98, (byte) 0x07, (byte) 0x01, };
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void parseInfo21(Application app, Iso7816.Response data, int dec, boolean bigEndian) {

		if (!data.isOkey() || data.size() < 30) {
			return;
		}

		final byte[] d = data.getBytes();

		if (d[2] == 0x20 && d[3] == 0x00) {
			applicationId = SPEC.APP.SHANGHAIGJ;
			bigEndian = true;
		} else if (d[2] == 0x71 && d[3] == 0x00) {
			applicationId = SPEC.APP.CHANGANTONG;
			bigEndian = false;
		} else {
			applicationId = SPEC.getCityUnionCardNameByZipcode(Util.toHexString(d[2], d[3]));
			bigEndian = false;
		}

		if (dec < 1 || dec > 10) {
			app.setProperty(SPEC.PROP.SERIAL, Util.toHexString(d, 10, 10));
		} else {
			final int sn = Util.toInt(d, 20 - dec, dec);
			final String ss = bigEndian ? Util.toStringR(sn) : String
					.format("%d", 0xFFFFFFFFL & sn);
			app.setProperty(SPEC.PROP.SERIAL, ss);
		}

		if (d[9] != 0)
			app.setProperty(SPEC.PROP.VERSION, String.valueOf(d[9]));

		app.setProperty(SPEC.PROP.DATE, String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X",
				d[20], d[21], d[22], d[23], d[24], d[25], d[26], d[27]));
	}
}
