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

import java.io.IOException;
import java.util.ArrayList;

import com.sinpo.xnfc.SPEC;
import com.sinpo.xnfc.nfc.Util;
import com.sinpo.xnfc.nfc.bean.Application;
import com.sinpo.xnfc.nfc.bean.Card;
import com.sinpo.xnfc.nfc.tech.Iso7816;

final class BeijingMunicipal extends StandardPboc {

	@Override
	protected SPEC.APP getApplicationId() {
		return SPEC.APP.BEIJINGMUNICIPAL;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

		Iso7816.Response INFO, CNT, BALANCE;

		/*--------------------------------------------------------------*/
		// read card info file, binary (4)
		/*--------------------------------------------------------------*/
		INFO = tag.readBinary(SFI_EXTRA_LOG);
		if (!INFO.isOkey())
			return HINT.GONEXT;

		/*--------------------------------------------------------------*/
		// read card operation file, binary (5)
		/*--------------------------------------------------------------*/
		CNT = tag.readBinary(SFI_EXTRA_CNT);

		/*--------------------------------------------------------------*/
		// select Main Application
		/*--------------------------------------------------------------*/
		if (!tag.selectByID(DFI_EP).isOkey())
			return HINT.RESETANDGONEXT;

		BALANCE = tag.getBalance(0, true);

		/*--------------------------------------------------------------*/
		// read log file, record (24)
		/*--------------------------------------------------------------*/
		ArrayList<byte[]> LOG = readLog24(tag, SFI_LOG);

		/*--------------------------------------------------------------*/
		// build result
		/*--------------------------------------------------------------*/
		final Application app = createApplication();

		parseBalance(app, BALANCE);

		parseInfo4(app, INFO, CNT);

		parseLog24(app, LOG);

		configApplication(app);

		card.addApplication(app);

		return HINT.STOP;
	}

	private final static int SFI_EXTRA_LOG = 4;
	private final static int SFI_EXTRA_CNT = 5;

	private void parseInfo4(Application app, Iso7816.Response info,
			Iso7816.Response cnt) {

		if (!info.isOkey() || info.size() < 32) {
			return;
		}

		final byte[] d = info.getBytes();
		app.setProperty(SPEC.PROP.SERIAL, Util.toHexString(d, 0, 8));
		app.setProperty(SPEC.PROP.VERSION,
				String.format("%02X.%02X%02X", d[8], d[9], d[10]));
		app.setProperty(SPEC.PROP.DATE, String.format(
				"%02X%02X.%02X.%02X - %02X%02X.%02X.%02X", d[24], d[25], d[26],
				d[27], d[28], d[29], d[30], d[31]));

		if (cnt != null && cnt.isOkey() && cnt.size() > 4) {
			byte[] e = cnt.getBytes();
			final int n = Util.toInt(e, 1, 4);
			if (e[0] == 0)
				app.setProperty(SPEC.PROP.COUNT, String.format("%d", n));
			else
				app.setProperty(SPEC.PROP.COUNT, String.format("%d*", n));
		}
	}
}
