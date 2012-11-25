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
import com.sinpo.xnfc.tech.Iso7816.BerT;

final class HardReader extends PbocCard {
	public static final byte TMPL_PDR = 0x70; // Payment Directory Entry Record
	public static final byte TMPL_PDE = 0x61; // Payment Directory Entry

	private HardReader(Iso7816.Tag tag, byte[] name, Resources res) {
		super(tag);
		this.name = (name != null) ? Util.toHexString(name, 0, name.length)
				: res.getString(R.string.name_unknowntag);
	}

	@SuppressWarnings("unchecked")
	final static HardReader load(Iso7816.Tag tag, Resources res) {

		/*--------------------------------------------------------------*/
		// select PSF (1PAY.SYS.DDF01)
		/*--------------------------------------------------------------*/
		if (!tag.selectByName(DFN_PSE).isOkey() && !tag.selectByID(DFI_MF).isOkey())
			return null;		
		
		/*--------------------------------------------------------------*/
		// read balance
		/*--------------------------------------------------------------*/
		Iso7816.Response CASH = getBalance(tag);

		Iso7816.Response INFO = null;
		ArrayList<byte[]> LOG = new ArrayList<byte[]>();
		byte[] name = null;

		/*--------------------------------------------------------------*/
		// try to find AID list
		/*--------------------------------------------------------------*/
		ArrayList<byte[]> AIDs = findAIDs(tag);
		for (final byte[] aid : AIDs) {

			/*--------------------------------------------------------------*/
			// select Main Application
			/*--------------------------------------------------------------*/
			if ((name = selectAID(tag, aid)) != null) {
				/*--------------------------------------------------------------*/
				// read balance
				/*--------------------------------------------------------------*/
				if (!CASH.isOkey())
					CASH = getBalance(tag);

				/*--------------------------------------------------------------*/
				// read card info file, binary (21)
				/*--------------------------------------------------------------*/
				if (INFO == null || !INFO.isOkey())
					INFO = tag.readBinary(SFI_EXTRA);

				/*--------------------------------------------------------------*/
				// read log file, record (24)
				/*--------------------------------------------------------------*/
				LOG.addAll(readLog(tag, SFI_LOG));
			}
		}

		/*--------------------------------------------------------------*/
		// try to PXX AID
		/*--------------------------------------------------------------*/
		if ((INFO == null || !INFO.isOkey())
				&& ((name = selectAID(tag, DFN_PXX)) != null)) {

			if (!CASH.isOkey())
				CASH = getBalance(tag);

			INFO = tag.readBinary(SFI_EXTRA);
			LOG.addAll(readLog(tag, SFI_LOG));
		}

		/*--------------------------------------------------------------*/
		// try to 0x1001 AID
		/*--------------------------------------------------------------*/
		if ((INFO == null || !INFO.isOkey()) && tag.selectByID(DFI_EP).isOkey()) {
			name = DFI_EP;

			if (!CASH.isOkey())
				CASH = getBalance(tag);

			INFO = tag.readBinary(SFI_EXTRA);
			LOG.addAll(readLog(tag, SFI_LOG));
		}

		if (!CASH.isOkey() && INFO == null && LOG.isEmpty() && name == null)
			return null;

		/*--------------------------------------------------------------*/
		// build result string
		/*--------------------------------------------------------------*/
		final HardReader ret = new HardReader(tag, name, res);
		ret.parseBalance(CASH);

		if (INFO != null)
			ret.parseInfo(INFO, 0, false);

		ret.parseLog(LOG);

		return ret;
	}

	private static byte[] selectAID(Iso7816.Tag tag, byte[] aid) {
		if (!tag.selectByName(DFN_PSE).isOkey()
				&& !tag.selectByID(DFI_MF).isOkey())
			return null;

		final Iso7816.Response rsp = tag.selectByName(aid);
		if (!rsp.isOkey())
			return null;

		Iso7816.BerTLV tlv = Iso7816.BerTLV.read(rsp);
		if (tlv.t.match(Iso7816.BerT.TMPL_FCI)) {
			tlv = tlv.getChildByTag(Iso7816.BerT.CLASS_DFN);
			if (tlv != null)
				return tlv.v.getBytes();
		}

		return aid;
	}

	private static ArrayList<byte[]> findAIDs(Iso7816.Tag tag) {
		ArrayList<byte[]> ret = new ArrayList<byte[]>();

		for (int i = 1; i <= 31; ++i) {
			Iso7816.Response r = tag.readRecord(i, 1);
			for (int p = 2; r.isOkey(); ++p) {
				byte[] aid = findAID(r);
				if (aid == null)
					break;

				ret.add(aid);
				r = tag.readRecord(i, p);
			}
		}

		return ret;
	}

	private static byte[] findAID(Iso7816.Response record) {
		Iso7816.BerTLV tlv = Iso7816.BerTLV.read(record);
		if (tlv.t.match(TMPL_PDR)) {
			tlv = tlv.getChildByTag(BerT.CLASS_ADO);
			if (tlv != null) {
				tlv = tlv.getChildByTag(BerT.CLASS_AID);

				return (tlv != null) ? tlv.v.getBytes() : null;
			}
		}
		return null;
	}

	private static Iso7816.Response getBalance(Iso7816.Tag tag) {
		final Iso7816.Response rsp = tag.getBalance(true);
		return rsp.isOkey() ? rsp : tag.getBalance(false);
	}
}
