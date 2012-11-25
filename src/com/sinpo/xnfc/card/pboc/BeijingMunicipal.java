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

package com.sinpo.xnfc.card.pboc;

import java.util.ArrayList;

import android.content.res.Resources;

import com.sinpo.xnfc.R;
import com.sinpo.xnfc.Util;
import com.sinpo.xnfc.tech.Iso7816;

final class BeijingMunicipal extends PbocCard {
	private final static int SFI_EXTRA_LOG = 4;
	private final static int SFI_EXTRA_CNT = 5;

	private BeijingMunicipal(Iso7816.Tag tag, Resources res) {
		super(tag);
		name = res.getString(R.string.name_bj);
	}

	@SuppressWarnings("unchecked")
	final static BeijingMunicipal load(Iso7816.Tag tag, Resources res) {

		/*--------------------------------------------------------------*/
		// select PSF (1PAY.SYS.DDF01)
		/*--------------------------------------------------------------*/
		if (tag.selectByName(DFN_PSE).isOkey()) {

			Iso7816.Response INFO, CNT, CASH;

			/*--------------------------------------------------------------*/
			// read card info file, binary (4)
			/*--------------------------------------------------------------*/
			INFO = tag.readBinary(SFI_EXTRA_LOG);
			if (INFO.isOkey()) {

				/*--------------------------------------------------------------*/
				// read card operation file, binary (5)
				/*--------------------------------------------------------------*/
				CNT = tag.readBinary(SFI_EXTRA_CNT);

				/*--------------------------------------------------------------*/
				// select Main Application
				/*--------------------------------------------------------------*/
				if (tag.selectByID(DFI_EP).isOkey()) {

					/*--------------------------------------------------------------*/
					// read balance
					/*--------------------------------------------------------------*/
					CASH = tag.getBalance(true);

					/*--------------------------------------------------------------*/
					// read log file, record (24)
					/*--------------------------------------------------------------*/
					ArrayList<byte[]> LOG = readLog(tag, SFI_LOG);

					/*--------------------------------------------------------------*/
					// build result string
					/*--------------------------------------------------------------*/
					final BeijingMunicipal ret = new BeijingMunicipal(tag, res);
					ret.parseBalance(CASH);
					ret.parseInfo(INFO, CNT);
					ret.parseLog(LOG);

					return ret;
				}
			}
		}

		return null;
	}

	private void parseInfo(Iso7816.Response info, Iso7816.Response cnt) {
		if (!info.isOkey() || info.size() < 32) {
			serl = version = date = count = null;
			return;
		}

		final byte[] d = info.getBytes();
		serl = Util.toHexString(d, 0, 8);
		version = String.format("%02X.%02X%02X", d[8], d[9], d[10]);
		date = String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X", d[24],
				d[25], d[26], d[27], d[28], d[29], d[30], d[31]);
		count = null;

		if (cnt != null && cnt.isOkey() && cnt.size() > 4) {
			byte[] e = cnt.getBytes();
			final int n = Util.toInt(e, 1, 4);
			if (e[0] == 0)
				count = String.format("%d ", n);
			else
				count = String.format("%d* ", n);
		}
	}
}
