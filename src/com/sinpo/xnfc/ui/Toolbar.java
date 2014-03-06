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

package com.sinpo.xnfc.ui;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sinpo.xnfc.R;
import com.sinpo.xnfc.ThisApplication;

public final class Toolbar {
	final ViewGroup toolbar;

	@SuppressLint("NewApi")
	public Toolbar(ViewGroup toolbar) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			toolbar.setLayoutTransition(new LayoutTransition());

		this.toolbar = toolbar;
	}

	public void copyPageContent(TextView textArea) {
		final CharSequence text = textArea.getText();
		if (!TextUtils.isEmpty(text)) {
			((ClipboardManager) textArea.getContext().getSystemService(
					Context.CLIPBOARD_SERVICE)).setText(text.toString());

			ThisApplication.showMessage(R.string.info_main_copied);
		}
	}

	public void sharePageContent(TextView textArea) {
		final CharSequence text = textArea.getText();
		if (!TextUtils.isEmpty(text)) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_SUBJECT, ThisApplication.name());
			intent.putExtra(Intent.EXTRA_TEXT, text.toString());
			intent.setType("text/plain");
			textArea.getContext().startActivity(intent);
		}
	}

	public void show(int... buttons) {
		hide();

		showDelayed(1000, buttons);
	}

	private void hide() {
		final int n = toolbar.getChildCount();
		for (int i = 0; i < n; ++i)
			toolbar.getChildAt(i).setVisibility(View.GONE);
	}

	private void showDelayed(int delay, int... buttons) {
		toolbar.postDelayed(new Helper(buttons), delay);
	}

	private final class Helper implements Runnable {

		private final int[] buttons;

		Helper(int... buttons) {
			this.buttons = buttons;
		}

		@Override
		public void run() {
			final int n = toolbar.getChildCount();
			for (int i = 0; i < n; ++i) {
				final View view = toolbar.getChildAt(i);

				int visibility = View.GONE;
				if (buttons != null) {
					final int id = view.getId();
					for (int btn : buttons) {
						if (btn == id) {
							visibility = View.VISIBLE;
							break;
						}
					}
				}

				view.setVisibility(visibility);
			}
		}
	}
}
