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

final class YangchengTong extends PbocCard {
	private final static byte[] DFN_SRV = { (byte) 'P', (byte) 'A', (byte) 'Y',
			(byte) '.', (byte) 'A', (byte) 'P', (byte) 'P', (byte) 'Y', };

	private final static byte[] DFN_SRV_S1 = { (byte) 'P', (byte) 'A',
			(byte) 'Y', (byte) '.', (byte) 'P', (byte) 'A', (byte) 'S',
			(byte) 'D', };

	private final static byte[] DFN_SRV_S2 = { (byte) 'P', (byte) 'A',
			(byte) 'Y', (byte) '.', (byte) 'T', (byte) 'I', (byte) 'C',
			(byte) 'L', };

	private YangchengTong(Iso7816.Tag tag, Resources res) {
		super(tag);
		name = res.getString(R.string.name_lnt);
	}

	@SuppressWarnings("unchecked")
	final static YangchengTong load(Iso7816.Tag tag, Resources res) {

		/*--------------------------------------------------------------*/
		// select PSF (1PAY.SYS.DDF01)
		/*--------------------------------------------------------------*/
		if (tag.selectByName(DFN_PSE).isOkey()) {

			Iso7816.Response INFO, CASH;

			/*--------------------------------------------------------------*/
			// select Main Application
			/*--------------------------------------------------------------*/
			if (tag.selectByName(DFN_SRV).isOkey()) {

				/*--------------------------------------------------------------*/
				// read card info file, binary (21)
				/*--------------------------------------------------------------*/
				INFO = tag.readBinary(SFI_EXTRA);

				/*--------------------------------------------------------------*/
				// read balance
				/*--------------------------------------------------------------*/
				CASH = tag.getBalance(true);

				/*--------------------------------------------------------------*/
				// read log file, record (24)
				/*--------------------------------------------------------------*/
				ArrayList<byte[]> LOG1 = (tag.selectByName(DFN_SRV_S1).isOkey()) ? readLog(
						tag, SFI_LOG) : null;

				ArrayList<byte[]> LOG2 = (tag.selectByName(DFN_SRV_S2).isOkey()) ? readLog(
						tag, SFI_LOG) : null;

				/*--------------------------------------------------------------*/
				// build result string
				/*--------------------------------------------------------------*/
				final YangchengTong ret = new YangchengTong(tag, res);
				ret.parseBalance(CASH);
				ret.parseInfo(INFO);
				ret.parseLog(LOG1, LOG2);

				return ret;
			}
		}

		return null;
	}

	private void parseInfo(Iso7816.Response info) {
		if (!info.isOkey() || info.size() < 50) {
			serl = version = date = count = null;
			return;
		}

		final byte[] d = info.getBytes();
		serl = Util.toHexString(d, 11, 5);
		version = String.format("%02X.%02X", d[44], d[45]);
		date = String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X", d[23],
				d[24], d[25], d[26], d[27], d[28], d[29], d[30]);
		count = null;
	}
}
