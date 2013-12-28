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

package com.sinpo.xnfc.nfc;

import static android.nfc.NfcAdapter.EXTRA_TAG;
import static android.os.Build.VERSION_CODES.GINGERBREAD_MR1;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;

import com.sinpo.xnfc.nfc.reader.ReaderListener;
import com.sinpo.xnfc.nfc.reader.ReaderManager;

public final class NfcManager {

	private final Activity activity;
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;

	private static String[][] TECHLISTS;
	private static IntentFilter[] TAGFILTERS;
	private int status;

	static {
		try {
			TECHLISTS = new String[][] { { IsoDep.class.getName() },
					{ NfcF.class.getName() }, };

			TAGFILTERS = new IntentFilter[] { new IntentFilter(
					NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception e) {
		}
	}

	public NfcManager(Activity activity) {
		this.activity = activity;
		nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		pendingIntent = PendingIntent.getActivity(activity, 0, new Intent(
				activity, activity.getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		setupBeam(true);

		status = getStatus();
	}

	public void onPause() {
		setupOldFashionBeam(false);

		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(activity);
	}

	public void onResume() {
		setupOldFashionBeam(true);

		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(activity, pendingIntent,
					TAGFILTERS, TECHLISTS);
	}

	public boolean updateStatus() {

		int sta = getStatus();
		if (sta != status) {
			status = sta;
			return true;
		}

		return false;
	}

	public boolean readCard(Intent intent, ReaderListener listener) {
		final Tag tag = (Tag) intent.getParcelableExtra(EXTRA_TAG);
		if (tag != null) {
			ReaderManager.readCard(tag, listener);
			return true;
		}
		return false;
	}

	private int getStatus() {
		return (nfcAdapter == null) ? -1 : nfcAdapter.isEnabled() ? 1 : 0;
	}

	@SuppressLint("NewApi")
	private void setupBeam(boolean enable) {

		final int api = android.os.Build.VERSION.SDK_INT;
		if (nfcAdapter != null && api >= ICE_CREAM_SANDWICH) {
			if (enable)
				nfcAdapter.setNdefPushMessage(createNdefMessage(), activity);
		}
	}

	@SuppressWarnings("deprecation")
	private void setupOldFashionBeam(boolean enable) {

		final int api = android.os.Build.VERSION.SDK_INT;
		if (nfcAdapter != null && api >= GINGERBREAD_MR1
				&& api < ICE_CREAM_SANDWICH) {

			if (enable)
				nfcAdapter.enableForegroundNdefPush(activity,
						createNdefMessage());
			else
				nfcAdapter.disableForegroundNdefPush(activity);
		}
	}

	NdefMessage createNdefMessage() {

		String uri = "3play.google.com/store/apps/details?id=com.sinpo.xnfc";
		byte[] data = uri.getBytes();

		// about this '3'.. see NdefRecord.createUri which need api level 14
		data[0] = 3;

		NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_URI, null, data);

		return new NdefMessage(new NdefRecord[] { record });
	}
}
