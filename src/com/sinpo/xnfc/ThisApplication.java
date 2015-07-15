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

package com.sinpo.xnfc;

import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;

import com.sinpo.xnfc.R;

import android.app.Application;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.widget.Toast;

public final class ThisApplication extends Application implements UncaughtExceptionHandler {
	private static ThisApplication instance;

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		System.exit(0);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(this);

		instance = this;
	}

	public static String name() {
		return getStringResource(R.string.app_name);
	}

	public static String version() {
		try {
			return instance.getPackageManager().getPackageInfo(instance.getPackageName(), 0).versionName;
		} catch (Exception e) {
			return "1.0";
		}
	}

	public static void showMessage(int fmt, CharSequence... msgs) {
		String msg = String.format(getStringResource(fmt), msgs);
		Toast.makeText(instance, msg, Toast.LENGTH_LONG).show();
	}

	public static Typeface getFontResource(int pathId) {
		String path = getStringResource(pathId);
		return Typeface.createFromAsset(instance.getAssets(), path);
	}

	public static int getDimensionResourcePixelSize(int resId) {
		return instance.getResources().getDimensionPixelSize(resId);
	}

	public static int getColorResource(int resId) {
		return instance.getResources().getColor(resId);
	}

	public static String getStringResource(int resId) {
		return instance.getString(resId);
	}

	public static Drawable getDrawableResource(int resId) {
		return instance.getResources().getDrawable(resId);
	}

	public static DisplayMetrics getDisplayMetrics() {
		return instance.getResources().getDisplayMetrics();
	}

	public static byte[] loadRawResource(int resId) {
		InputStream is = null;
		try {
			is = instance.getResources().openRawResource(resId);

			int len = is.available();
			byte[] raw = new byte[(int) len];

			int offset = 0;
			while (offset < raw.length) {
				int n = is.read(raw, offset, raw.length - offset);
				if (n < 0)
					break;

				offset += n;
			}
			return raw;
		} catch (Throwable e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (Throwable ee) {
			}
		}
	}
}
