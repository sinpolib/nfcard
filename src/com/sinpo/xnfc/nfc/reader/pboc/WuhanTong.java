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

final class WuhanTong extends StandardPboc {

	@Override
	protected SPEC.APP getApplicationId() {
		return SPEC.APP.WUHANTONG;
	}

	@Override
	protected byte[] getMainApplicationId() {
		return new byte[] { (byte) 0x41, (byte) 0x50, (byte) 0x31, (byte) 0x2E,
				(byte) 0x57, (byte) 0x48, (byte) 0x43, (byte) 0x54,
				(byte) 0x43, };
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

		Iso7816.Response INFO, SERL, BALANCE;

		/*--------------------------------------------------------------*/
		// read card info file, binary (5, 10)
		/*--------------------------------------------------------------*/
		if (!(SERL = tag.readBinary(SFI_SERL)).isOkey())
			return HINT.GONEXT;

		if (!(INFO = tag.readBinary(SFI_INFO)).isOkey())
			return HINT.GONEXT;

		BALANCE = tag.getBalance(true);

		/*--------------------------------------------------------------*/
		// select Main Application
		/*--------------------------------------------------------------*/
		if (!tag.selectByName(getMainApplicationId()).isOkey())
			return HINT.RESETANDGONEXT;

		/*--------------------------------------------------------------*/
		// read balance
		/*--------------------------------------------------------------*/
		if (!BALANCE.isOkey())
			BALANCE = tag.getBalance(true);

		/*--------------------------------------------------------------*/
		// read log file, record (24)
		/*--------------------------------------------------------------*/
		ArrayList<byte[]> LOG = readLog24(tag, SFI_LOG);

		/*--------------------------------------------------------------*/
		// build result
		/*--------------------------------------------------------------*/
		final Application app = createApplication();

		parseBalance(app, BALANCE);

		parseInfo5(app, SERL, INFO);

		parseLog24(app, LOG);

		configApplication(app);

		card.addApplication(app);

		return HINT.STOP;
	}

	private final static int SFI_INFO = 5;
	private final static int SFI_SERL = 10;

	private void parseInfo5(Application app, Iso7816.Response sn,
			Iso7816.Response info) {
		if (sn.size() < 27 || info.size() < 27) {
			return;
		}

		final byte[] d = info.getBytes();
		app.setProperty(SPEC.PROP.SERIAL, Util.toHexString(sn.getBytes(), 0, 5));
		app.setProperty(SPEC.PROP.VERSION, String.format("%02d", d[24]));
		app.setProperty(SPEC.PROP.DATE, String.format(
				"%02X%02X.%02X.%02X - %02X%02X.%02X.%02X", d[20], d[21], d[22],
				d[23], d[16], d[17], d[18], d[19]));
	}
}
