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
import java.util.Arrays;

import com.sinpo.xnfc.SPEC;
import com.sinpo.xnfc.nfc.Util;
import com.sinpo.xnfc.nfc.bean.Application;
import com.sinpo.xnfc.nfc.bean.Card;
import com.sinpo.xnfc.nfc.tech.Iso7816;

import android.nfc.tech.IsoDep;

@SuppressWarnings("unchecked")
public abstract class StandardPboc {
	private static Class<?>[][] readers = {
			{ BeijingMunicipal.class, WuhanTong.class, CityUnion.class, TUnion.class,
					ShenzhenTong.class, }, { StandardECash.class, } };

	public static void readCard(IsoDep tech, Card card) throws InstantiationException,
			IllegalAccessException, IOException {

		final Iso7816.StdTag tag = new Iso7816.StdTag(tech);

		tag.connect();

		for (final Class<?> g[] : readers) {
			HINT hint = HINT.RESETANDGONEXT;

			for (final Class<?> r : g) {

				final StandardPboc reader = (StandardPboc) r.newInstance();

				switch (hint) {

				case RESETANDGONEXT:
					if (!reader.resetTag(tag))
						continue;

				case GONEXT:
					hint = reader.readCard(tag, card);
					break;

				default:
					break;
				}

				if (hint == HINT.STOP)
					break;
			}
		}

		tag.close();
	}

	protected boolean resetTag(Iso7816.StdTag tag) throws IOException {
		return tag.selectByID(DFI_MF).isOkey() || tag.selectByName(DFN_PSE).isOkey();
	}

	protected enum HINT {
		STOP, GONEXT, RESETANDGONEXT,
	}

	protected final static byte[] DFI_MF = { (byte) 0x3F, (byte) 0x00 };
	protected final static byte[] DFI_EP = { (byte) 0x10, (byte) 0x01 };

	protected final static byte[] DFN_PSE = { (byte) '1', (byte) 'P', (byte) 'A', (byte) 'Y',
			(byte) '.', (byte) 'S', (byte) 'Y', (byte) 'S', (byte) '.', (byte) 'D', (byte) 'D',
			(byte) 'F', (byte) '0', (byte) '1', };

	protected final static byte[] DFN_PXX = { (byte) 'P' };

	protected final static int SFI_EXTRA = 21;

	protected static int MAX_LOG = 10;
	protected static int SFI_LOG = 24;

	protected final static byte TRANS_CSU = 6;
	protected final static byte TRANS_CSU_CPX = 9;

	protected abstract Object getApplicationId();

	protected byte[] getMainApplicationId() {
		return DFI_EP;
	}

	protected SPEC.CUR getCurrency() {
		return SPEC.CUR.CNY;
	}

	protected boolean selectMainApplication(Iso7816.StdTag tag) throws IOException {
		final byte[] aid = getMainApplicationId();
		return ((aid.length == 2) ? tag.selectByID(aid) : tag.selectByName(aid)).isOkey();
	}

	protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

		/*--------------------------------------------------------------*/
		// select Main Application
		/*--------------------------------------------------------------*/
		if (!selectMainApplication(tag))
			return HINT.GONEXT;

		Iso7816.Response INFO, BALANCE;

		/*--------------------------------------------------------------*/
		// read card info file, binary (21)
		/*--------------------------------------------------------------*/
		INFO = tag.readBinary(SFI_EXTRA);

		/*--------------------------------------------------------------*/
		// read balance
		/*--------------------------------------------------------------*/
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

		parseInfo21(app, INFO, 4, true);

		parseLog24(app, LOG);

		configApplication(app);

		card.addApplication(app);

		return HINT.STOP;
	}

	protected float parseBalance(Iso7816.Response data) {
		float ret = 0f;
		if (data.isOkey() && data.size() >= 4) {
			int n = Util.toInt(data.getBytes(), 0, 4);
			if (n > 1000000 || n < -1000000)
				n -= 0x80000000;

			ret = n / 100.0f;
		}
		return ret;
	}

	protected void parseBalance(Application app, Iso7816.Response... data) {

		float amount = 0f;
		for (Iso7816.Response rsp : data)
			amount += parseBalance(rsp);

		app.setProperty(SPEC.PROP.BALANCE, amount);
	}

	protected void parseInfo21(Application app, Iso7816.Response data, int dec, boolean bigEndian) {
		if (!data.isOkey() || data.size() < 30) {
			return;
		}

		final byte[] d = data.getBytes();
		if (dec < 1 || dec > 10) {
			app.setProperty(SPEC.PROP.SERIAL, Util.toHexString(d, 10, 10));
		} else {
			final int sn = bigEndian ? Util.toIntR(d, 19, dec) : Util.toInt(d, 20 - dec, dec);

			app.setProperty(SPEC.PROP.SERIAL, String.format("%d", 0xFFFFFFFFL & sn));
		}

		if (d[9] != 0)
			app.setProperty(SPEC.PROP.VERSION, String.valueOf(d[9]));

		app.setProperty(SPEC.PROP.DATE, String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X",
				d[20], d[21], d[22], d[23], d[24], d[25], d[26], d[27]));
	}

	protected boolean addLog24(final Iso7816.Response r, ArrayList<byte[]> l) {
		if (!r.isOkey())
			return false;

		final byte[] raw = r.getBytes();
		final int N = raw.length - 23;
		if (N < 0)
			return false;

		for (int s = 0, e = 0; s <= N; s = e) {
			l.add(Arrays.copyOfRange(raw, s, (e = s + 23)));
		}

		return true;
	}

	protected ArrayList<byte[]> readLog24(Iso7816.StdTag tag, int sfi) throws IOException {
		final ArrayList<byte[]> ret = new ArrayList<byte[]>(MAX_LOG);
		final Iso7816.Response rsp = tag.readRecord(sfi);
		if (rsp.isOkey()) {
			addLog24(rsp, ret);
		} else {
			for (int i = 1; i <= MAX_LOG; ++i) {
				if (!addLog24(tag.readRecord(sfi, i), ret))
					break;
			}
		}

		return ret;
	}

	protected void parseLog24(Application app, ArrayList<byte[]>... logs) {
		final ArrayList<String> ret = new ArrayList<String>(MAX_LOG);

		for (final ArrayList<byte[]> log : logs) {
			if (log == null)
				continue;

			for (final byte[] v : log) {
				final int money = Util.toInt(v, 5, 4);
				if (money > 0) {
					final char s = (v[9] == TRANS_CSU || v[9] == TRANS_CSU_CPX) ? '-' : '+';

					final int over = Util.toInt(v, 2, 3);
					final String slog;
					if (over > 0) {
						slog = String
								.format("%02X%02X.%02X.%02X %02X:%02X %c%.2f [o:%.2f] [%02X%02X%02X%02X%02X%02X]",
										v[16], v[17], v[18], v[19], v[20], v[21], s,
										(money / 100.0f), (over / 100.0f), v[10], v[11], v[12],
										v[13], v[14], v[15]);
					} else {
						slog = String.format(
								"%02X%02X.%02X.%02X %02X:%02X %C%.2f [%02X%02X%02X%02X%02X%02X]",
								v[16], v[17], v[18], v[19], v[20], v[21], s, (money / 100.0f),
								v[10], v[11], v[12], v[13], v[14], v[15]);

					}

					ret.add(slog);
				}
			}
		}

		if (!ret.isEmpty())
			app.setProperty(SPEC.PROP.TRANSLOG, ret.toArray(new String[ret.size()]));
	}

	protected Application createApplication() {
		return new Application();
	}

	protected void configApplication(Application app) {
		app.setProperty(SPEC.PROP.ID, getApplicationId());
		app.setProperty(SPEC.PROP.CURRENCY, getCurrency());
	}
}
