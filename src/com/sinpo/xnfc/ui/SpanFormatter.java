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

import java.lang.ref.WeakReference;

import org.xml.sax.XMLReader;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.util.DisplayMetrics;
import android.view.View;

import com.sinpo.xnfc.R;
import com.sinpo.xnfc.SPEC;
import com.sinpo.xnfc.ThisApplication;

public final class SpanFormatter implements Html.TagHandler {
	public interface ActionHandler {
		void handleAction(CharSequence name);
	}

	private final ActionHandler handler;

	public SpanFormatter(ActionHandler handler) {
		this.handler = handler;
	}

	public CharSequence toSpanned(String html) {
		return Html.fromHtml(html, null, this);
	}

	private static final class ActionSpan extends ClickableSpan {
		private final String action;
		private final ActionHandler handler;
		private final int color;

		ActionSpan(String action, ActionHandler handler, int color) {
			this.action = action;
			this.handler = handler;
			this.color = color;
		}

		@Override
		public void onClick(View widget) {
			if (handler != null)
				handler.handleAction(action);
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			super.updateDrawState(ds);
			ds.setColor(color);
		}
	}

	private static final class FontSpan extends MetricAffectingSpan {

		final int color;
		final float size;
		final Typeface face;
		final boolean bold;

		FontSpan(int color, float size, Typeface face) {
			this.color = color;
			this.size = size;

			if (face == Typeface.DEFAULT) {
				this.face = null;
				this.bold = false;
			} else if (face == Typeface.DEFAULT_BOLD) {
				this.face = null;
				this.bold = true;
			} else {
				this.face = face;
				this.bold = false;
			}
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			ds.setTextSize(size);
			ds.setColor(color);

			if (face != null) {
				ds.setTypeface(face);
			} else if (bold) {
				Typeface tf = ds.getTypeface();

				if (tf != null) {
					int style = tf.getStyle() | Typeface.BOLD;
					tf = Typeface.create(tf, style);
					ds.setTypeface(tf);

					style &= ~tf.getStyle();

					if ((style & Typeface.BOLD) != 0) {
						ds.setFakeBoldText(true);
					}
				}
			}
		}

		@Override
		public void updateMeasureState(TextPaint p) {
			updateDrawState(p);			
		}
	}

	private static final class ParagSpan implements LineHeightSpan {
		private final int linespaceDelta;

		ParagSpan(int linespaceDelta) {
			this.linespaceDelta = linespaceDelta;
		}

		@Override
		public void chooseHeight(CharSequence text, int start, int end,
				int spanstartv, int v, FontMetricsInt fm) {
			fm.bottom += linespaceDelta;
			fm.descent += linespaceDelta;
		}
	}

	private static final class SplitterSpan extends ReplacementSpan {
		private final int color;
		private final int width;
		private final int height;

		SplitterSpan(int color, int width, int height) {
			this.color = color;
			this.width = width;
			this.height = height;
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			ds.setTextSize(1);
		}

		@Override
		public int getSize(Paint paint, CharSequence text, int start, int end,
				Paint.FontMetricsInt fm) {
			return 0;
		}

		@Override
		public void draw(Canvas canvas, CharSequence text, int start, int end,
				float x, int top, int y, int bottom, Paint paint) {

			canvas.save();
			canvas.translate(x, (bottom + top) / 2 - height);

			final int c = paint.getColor();
			paint.setColor(color);
			canvas.drawRect(x, 0, x + width, height, paint);
			paint.setColor(c);

			canvas.restore();
		}
	}

	@Override
	public void handleTag(boolean opening, String tag, Editable output,
			XMLReader xmlReader) {

		final int len = output.length();

		if (opening) {

			if (SPEC.TAG_TEXT.equals(tag)) {
				markFontSpan(output, len, R.color.tag_text, R.dimen.tag_text,
						Typeface.DEFAULT);
			} else if (SPEC.TAG_TIP.equals(tag)) {
				markParagSpan(output, len, R.dimen.tag_parag);
				markFontSpan(output, len, R.color.tag_tip, R.dimen.tag_tip,
						getTipFont());
			} else if (SPEC.TAG_LAB.equals(tag)) {
				markFontSpan(output, len, R.color.tag_lab, R.dimen.tag_lab,
						Typeface.DEFAULT_BOLD);
			} else if (SPEC.TAG_ITEM.equals(tag)) {
				markFontSpan(output, len, R.color.tag_item, R.dimen.tag_item,
						Typeface.DEFAULT);
			} else if (SPEC.TAG_H1.equals(tag)) {
				markFontSpan(output, len, R.color.tag_h1, R.dimen.tag_h1,
						Typeface.DEFAULT_BOLD);
			} else if (SPEC.TAG_H2.equals(tag)) {
				markFontSpan(output, len, R.color.tag_h2, R.dimen.tag_h2,
						Typeface.DEFAULT_BOLD);
			} else if (SPEC.TAG_H3.equals(tag)) {
				markFontSpan(output, len, R.color.tag_h3, R.dimen.tag_h3,
						Typeface.SERIF);
			} else if (tag.startsWith(SPEC.TAG_ACT)) {
				markActionSpan(output, len, tag, R.color.tag_action);
			} else if (SPEC.TAG_PARAG.equals(tag)) {
				markParagSpan(output, len, R.dimen.tag_parag);
			} else if (SPEC.TAG_SP.equals(tag)) {
				markSpliterSpan(output, len, R.color.tag_action,
						R.dimen.tag_spliter);
			}
		} else {
			if (SPEC.TAG_TEXT.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (SPEC.TAG_TIP.equals(tag)) {
				setSpan(output, len, FontSpan.class);
				setSpan(output, len, ParagSpan.class);
			} else if (SPEC.TAG_LAB.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (SPEC.TAG_ITEM.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (SPEC.TAG_H1.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (SPEC.TAG_H2.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (SPEC.TAG_H3.equals(tag)) {
				setSpan(output, len, FontSpan.class);
			} else if (tag.startsWith(SPEC.TAG_ACT)) {
				setSpan(output, len, ActionSpan.class);
			} else if (SPEC.TAG_PARAG.equals(tag)) {
				setSpan(output, len, ParagSpan.class);
			}
		}
	}

	private static void markSpliterSpan(Editable out, int pos, int colorId,
			int heightId) {
		DisplayMetrics dm = ThisApplication.getDisplayMetrics();
		int color = ThisApplication.getColorResource(colorId);
		int height = ThisApplication.getDimensionResourcePixelSize(heightId);

		out.append("-------------------").setSpan(
				new SplitterSpan(color, dm.widthPixels, height), pos,
				out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private static void markFontSpan(Editable out, int pos, int colorId,
			int sizeId, Typeface face) {
		int color = ThisApplication.getColorResource(colorId);
		float size = ThisApplication.getDimensionResourcePixelSize(sizeId);
		FontSpan span = new FontSpan(color, size, face);
		out.setSpan(span, pos, pos, Spannable.SPAN_MARK_MARK);
	}

	private static void markParagSpan(Editable out, int pos, int linespaceId) {
		int linespace = ThisApplication
				.getDimensionResourcePixelSize(linespaceId);
		ParagSpan span = new ParagSpan(linespace);
		out.setSpan(span, pos, pos, Spannable.SPAN_MARK_MARK);
	}

	private void markActionSpan(Editable out, int pos, String tag, int colorId) {
		int color = ThisApplication.getColorResource(colorId);
		out.setSpan(new ActionSpan(tag, handler, color), pos, pos,
				Spannable.SPAN_MARK_MARK);
	}

	private static void setSpan(Editable out, int pos, Class<?> kind) {
		Object span = getLastMarkSpan(out, kind);
		out.setSpan(span, out.getSpanStart(span), pos,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private static Object getLastMarkSpan(Spanned text, Class<?> kind) {
		Object[] objs = text.getSpans(0, text.length(), kind);

		if (objs.length == 0) {
			return null;
		} else {
			return objs[objs.length - 1];
		}
	}

	private static Typeface getTipFont() {

		Typeface ret = null;

		WeakReference<Typeface> wr = TIPFONT;
		if (wr != null)
			ret = wr.get();

		if (ret == null) {
			ret = ThisApplication.getFontResource(R.string.font_oem3);
			TIPFONT = new WeakReference<Typeface>(ret);
		}

		return ret;
	}

	private static WeakReference<Typeface> TIPFONT;
}
