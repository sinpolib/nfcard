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

final class TUnion extends StandardPboc {

	@Override
	protected SPEC.APP getApplicationId() {
		return SPEC.APP.TUNIONEP;
	}

	protected boolean resetTag(Iso7816.StdTag tag) throws IOException {
		if (!tag.selectByID(DFI_MF).isOkey())
			tag.selectByName(DFN_PSE).isOkey();

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

		/*--------------------------------------------------------------*/
		// select Main Application
		/*--------------------------------------------------------------*/
		if (!selectMainApplication(tag))
			return HINT.GONEXT;

		Iso7816.Response INFO, BALANCE, OVER, OVER_LIMIT;

		/*--------------------------------------------------------------*/
		// read card info file, binary (21)
		/*--------------------------------------------------------------*/
		INFO = tag.readBinary(SFI_EXTRA);

		/*--------------------------------------------------------------*/
		// read balance
		/*--------------------------------------------------------------*/
		BALANCE = tag.getBalance(0x03, true);
		OVER = tag.getBalance(0x02, true);
		OVER_LIMIT = tag.getBalance(0x01, true);

		/*--------------------------------------------------------------*/
		// read log file, record (24)
		/*--------------------------------------------------------------*/
		ArrayList<byte[]> LOG = readLog24(tag, SFI_LOG);

		/*--------------------------------------------------------------*/
		// build result
		/*--------------------------------------------------------------*/
		final Application app = createApplication();

		parseBalance(app, BALANCE, OVER, OVER_LIMIT);

		parseInfo21(app, INFO, 4, true);

		parseLog24(app, LOG);

		configApplication(app);

		card.addApplication(app);

		return HINT.STOP;
	}

	@Override
	protected byte[] getMainApplicationId() {
		return new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x32,
				(byte) 0x01, (byte) 0x01, (byte) 0x05 };
	}

	@Override
	protected void parseInfo21(Application app, Iso7816.Response data, int dec, boolean bigEndian) {
		if (!data.isOkey() || data.size() < 30) {
			return;
		}

		final byte[] d = data.getBytes();
		String pan = Util.toHexString(d, 10, 10);
		app.setProperty(SPEC.PROP.SERIAL, pan.substring(1));

		if (d[9] != 0)
			app.setProperty(SPEC.PROP.VERSION, String.valueOf(d[9]));

		app.setProperty(SPEC.PROP.DATE, String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X",
				d[20], d[21], d[22], d[23], d[24], d[25], d[26], d[27]));
	}

	@Override
	protected void parseBalance(Application app, Iso7816.Response... data) {

		float balance = parseBalance(data[0]);
		if (balance < 0.01f) {
			float over = parseBalance(data[1]);
			if (over > 0.01f) {
				balance -= over;
				app.setProperty(SPEC.PROP.OLIMIT, parseBalance(data[2]));
			}
		}

		app.setProperty(SPEC.PROP.BALANCE, balance);
	}
}
