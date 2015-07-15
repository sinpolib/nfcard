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

package com.sinpo.xnfc.nfc.tech;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.sinpo.xnfc.nfc.Util;

import android.nfc.tech.IsoDep;

public class Iso7816 {
	public static final byte[] EMPTY = { 0 };

	protected byte[] data;

	protected Iso7816() {
		data = Iso7816.EMPTY;
	}

	protected Iso7816(byte[] bytes) {
		data = (bytes == null) ? Iso7816.EMPTY : bytes;
	}

	public boolean match(byte[] bytes) {
		return match(bytes, 0);
	}

	public boolean match(byte[] bytes, int start) {
		final byte[] data = this.data;
		if (data.length <= bytes.length - start) {
			for (final byte v : data) {
				if (v != bytes[start++])
					return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public boolean match(byte tag) {
		return (data.length == 1 && data[0] == tag);
	}

	public boolean match(short tag) {
		final byte[] data = this.data;
		if (data.length == 2) {
			final byte d0 = (byte) (0x000000FF & (tag >> 8));
			final byte d1 = (byte) (0x000000FF & tag);
			return (data[0] == d0 && data[1] == d1);
		}

		return (tag >= 0 && tag <= 255) ? match((byte) tag) : false;
	}

	public int size() {
		return data.length;
	}

	public byte[] getBytes() {
		return data;
	}

	public byte[] getBytes(int start, int count) {
		return Arrays.copyOfRange(data, start, start + count);
	}

	public int toInt() {
		return Util.toInt(getBytes());
	}

	public int toIntR() {
		return Util.toIntR(getBytes());
	}

	@Override
	public String toString() {
		return Util.toHexString(data, 0, data.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || !(obj instanceof Iso7816))
			return false;

		return match(((Iso7816) obj).getBytes(), 0);
	}

	public final static class ID extends Iso7816 {
		public ID(byte... bytes) {
			super(bytes);
		}
	}

	public static class Response extends Iso7816 {
		public static final byte[] EMPTY = {};
		public static final byte[] ERROR = { 0x6F, 0x00 }; // SW_UNKNOWN

		public Response(byte[] bytes) {
			super((bytes == null || bytes.length < 2) ? Response.ERROR : bytes);
		}

		public byte getSw1() {
			return data[data.length - 2];
		}

		public byte getSw2() {
			return data[data.length - 1];
		}

		public String getSw12String() {
			int sw1 = getSw1() & 0x000000FF;
			int sw2 = getSw2() & 0x000000FF;
			return String.format("0x%02X%02X", sw1, sw2);
		}

		public short getSw12() {
			final byte[] d = this.data;
			int n = d.length;
			return (short) ((d[n - 2] << 8) | (0xFF & d[n - 1]));
		}

		public boolean isOkey() {
			return equalsSw12(SW_NO_ERROR);
		}

		public boolean equalsSw12(short val) {
			return getSw12() == val;
		}

		public int size() {
			return data.length - 2;
		}

		public byte[] getBytes() {
			return isOkey() ? Arrays.copyOfRange(data, 0, size())
					: Response.EMPTY;
		}
	}

	public final static class MifareDResponse extends Response {
		public MifareDResponse(byte[] bytes) {
			super(bytes);
		}
	}

	public final static class BerT extends Iso7816 {
		// tag template
		public static final byte TMPL_FCP = 0x62; // File Control Parameters
		public static final byte TMPL_FMD = 0x64; // File Management Data
		public static final byte TMPL_FCI = 0x6F; // FCP and FMD

		// proprietary information
		public final static BerT CLASS_PRI = new BerT((byte) 0xA5);
		// short EF identifier
		public final static BerT CLASS_SFI = new BerT((byte) 0x88);
		// dedicated file name
		public final static BerT CLASS_DFN = new BerT((byte) 0x84);
		// application data object
		public final static BerT CLASS_ADO = new BerT((byte) 0x61);
		// application id
		public final static BerT CLASS_AID = new BerT((byte) 0x4F);

		// proprietary information

		public static int test(byte[] bytes, int start) {
			int len = 1;
			if ((bytes[start] & 0x1F) == 0x1F) {
				while ((bytes[start + len] & 0x80) == 0x80)
					++len;

				++len;
			}
			return len;
		}

		public static BerT read(byte[] bytes, int start) {
			return new BerT(Arrays.copyOfRange(bytes, start,
					start + test(bytes, start)));
		}

		public BerT(byte tag) {
			this(new byte[] { tag });
		}

		public BerT(short tag) {
			this(new byte[] { (byte) (0x000000FF & (tag >> 8)),
					(byte) (0x000000FF & tag) });
		}

		public BerT(byte[] bytes) {
			super(bytes);
		}

		public boolean hasChild() {
			return ((data[0] & 0x20) == 0x20);
		}

		public short toShort() {
			if (size() <= 2) {
				return (short) Util.toInt(data);
			}
			return 0;
		}
	}

	public final static class BerL extends Iso7816 {
		private final int val;

		public static int test(byte[] bytes, int start) {
			int len = 1;
			if ((bytes[start] & 0x80) == 0x80) {
				len += bytes[start] & 0x07;
			}
			return len;
		}

		public static int calc(byte[] bytes, int start) {
			if ((bytes[start] & 0x80) == 0x80) {
				int v = 0;

				int e = start + bytes[start] & 0x07;
				while (++start <= e) {
					v <<= 8;
					v |= bytes[start] & 0xFF;
				}

				return v;
			}

			return bytes[start];
		}

		public static BerL read(byte[] bytes, int start) {
			return new BerL(Arrays.copyOfRange(bytes, start,
					start + test(bytes, start)));
		}

		public BerL(byte[] bytes) {
			super(bytes);
			val = calc(bytes, 0);
		}

		public BerL(int len) {
			super(null);
			val = len;
		}

		public int toInt() {
			return val;
		}
	}

	public final static class BerV extends Iso7816 {
		public static BerV read(byte[] bytes, int start, int len) {
			return new BerV(Arrays.copyOfRange(bytes, start, start + len));
		}

		public BerV(byte[] bytes) {
			super(bytes);
		}
	}

	public final static class BerTLV extends Iso7816 {
		public static int test(byte[] bytes, int start) {
			final int lt = BerT.test(bytes, start);
			final int ll = BerL.test(bytes, start + lt);
			final int lv = BerL.calc(bytes, start + lt);

			return lt + ll + lv;
		}

		public static byte[] getValue(BerTLV tlv) {
			if (tlv == null || tlv.length() == 0)
				return null;

			return tlv.v.getBytes();
		}

		public static BerTLV read(Iso7816 obj) {
			return read(obj.getBytes(), 0);
		}

		public static BerTLV read(byte[] bytes, int start) {
			int s = start;
			final BerT t = BerT.read(bytes, s);
			s += t.size();

			final BerL l = BerL.read(bytes, s);
			s += l.size();

			final BerV v = BerV.read(bytes, s, l.toInt());
			s += v.size();

			final BerTLV tlv = new BerTLV(t, l, v);
			tlv.data = Arrays.copyOfRange(bytes, start, s);

			return tlv;
		}

		public static void extractChildren(ArrayList<BerTLV> out, Iso7816 obj) {
			extractChildren(out, obj.getBytes());
		}

		public static void extractChildren(ArrayList<BerTLV> out, byte[] data) {

			int start = 0;
			int end = data.length - 3;
			while (start <= end) {
				final BerTLV tlv = read(data, start);
				out.add(tlv);

				start += tlv.size();
			}
		}

		public static void extractPrimitives(BerHouse out, Iso7816 obj) {
			extractPrimitives(out.tlvs, obj.getBytes());
		}

		public static void extractPrimitives(ArrayList<BerTLV> out, Iso7816 obj) {
			extractPrimitives(out, obj.getBytes());
		}

		public static void extractPrimitives(BerHouse out, byte[] data) {
			extractPrimitives(out.tlvs, data);
		}

		public static void extractPrimitives(ArrayList<BerTLV> out, byte[] data) {

			int start = 0;
			int end = data.length - 3;
			while (start <= end) {
				final BerTLV tlv = read(data, start);
				if (tlv.t.hasChild())
					extractPrimitives(out, tlv.v.getBytes());
				else
					out.add(tlv);

				start += tlv.size();
			}
		}

		public static ArrayList<BerTLV> extractOptionList(byte[] data) {
			final ArrayList<BerTLV> ret = new ArrayList<BerTLV>();

			int start = 0;
			int end = data.length;
			while (start < end) {
				final BerT t = BerT.read(data, start);
				start += t.size();

				if (start < end) {
					BerL l = BerL.read(data, start);
					start += l.size();

					if (start <= end)
						ret.add(new BerTLV(t, l, null));
				}
			}

			return ret;
		}

		public final BerT t;
		public final BerL l;
		public final BerV v;

		public BerTLV(BerT t, BerL l, BerV v) {
			this.t = t;
			this.l = l;
			this.v = v;
		}

		public int length() {
			return l.toInt();
		}
	}

	public final static class BerHouse {
		final ArrayList<BerTLV> tlvs = new ArrayList<BerTLV>();

		public int count() {
			return tlvs.size();
		}

		public void add(short t, Response v) {
			tlvs.add(new BerTLV(new BerT(t), new BerL(v.size()), new BerV(v
					.getBytes())));
		}

		public void add(short t, byte[] v) {
			tlvs.add(new BerTLV(new BerT(t), new BerL(v.length), new BerV(v)));
		}

		public void add(BerT t, byte[] v) {
			tlvs.add(new BerTLV(t, new BerL(v.length), new BerV(v)));
		}

		public void add(BerTLV tlv) {
			tlvs.add(tlv);
		}

		public BerTLV get(int index) {
			return tlvs.get(index);
		}

		public BerTLV findFirst(byte tag) {
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					return tlv;

			return null;
		}

		public BerTLV findFirst(byte... tag) {
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					return tlv;

			return null;
		}

		public BerTLV findFirst(short tag) {
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					return tlv;

			return null;
		}

		public BerTLV findFirst(BerT tag) {
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag.getBytes()))
					return tlv;

			return null;
		}

		public ArrayList<BerTLV> findAll(byte tag) {
			final ArrayList<BerTLV> ret = new ArrayList<BerTLV>();
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					ret.add(tlv);

			return ret;
		}

		public ArrayList<BerTLV> findAll(byte... tag) {
			final ArrayList<BerTLV> ret = new ArrayList<BerTLV>();
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					ret.add(tlv);

			return ret;
		}

		public ArrayList<BerTLV> findAll(short tag) {
			final ArrayList<BerTLV> ret = new ArrayList<BerTLV>();
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag))
					ret.add(tlv);

			return ret;
		}

		public ArrayList<BerTLV> findAll(BerT tag) {
			final ArrayList<BerTLV> ret = new ArrayList<BerTLV>();
			for (BerTLV tlv : tlvs)
				if (tlv.t.match(tag.getBytes()))
					ret.add(tlv);

			return ret;
		}

		public String toString() {
			final StringBuilder ret = new StringBuilder();

			for (BerTLV t : tlvs) {
				ret.append(t.t.toString()).append(' ');
				ret.append(t.l.toInt()).append(' ');
				ret.append(t.v.toString()).append('\n');
			}

			return ret.toString();
		}
	}

	public final static class StdTag {
		private final IsoDep nfcTag;
		private ID id;

		public StdTag(IsoDep tag) {
			nfcTag = tag;
			id = new ID(tag.getTag().getId());
		}

		public ID getID() {
			return id;
		}

		public Response getBalance(int p1, boolean isEP) throws IOException {
			final byte[] cmd = { (byte) 0x80, // CLA Class
					(byte) 0x5C, // INS Instruction
					(byte) p1, // P1 Parameter 1
					(byte) (isEP ? 2 : 1), // P2 Parameter 2
					(byte) 0x04, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readRecord(int sfi, int index) throws IOException {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB2, // INS Instruction
					(byte) index, // P1 Parameter 1
					(byte) ((sfi << 3) | 0x04), // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readRecord(int sfi) throws IOException {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB2, // INS Instruction
					(byte) 0x01, // P1 Parameter 1
					(byte) ((sfi << 3) | 0x05), // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readBinary(int sfi) throws IOException {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB0, // INS Instruction
					(byte) (0x00000080 | (sfi & 0x1F)), // P1 Parameter 1
					(byte) 0x00, // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readData(int sfi) throws IOException {
			final byte[] cmd = { (byte) 0x80, // CLA Class
					(byte) 0xCA, // INS Instruction
					(byte) 0x00, // P1 Parameter 1
					(byte) (sfi & 0x1F), // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response getData(short tag) throws IOException {
			final byte[] cmd = {
					(byte) 0x80, // CLA Class
					(byte) 0xCA, // INS Instruction
					(byte) ((tag >> 8) & 0xFF), (byte) (tag & 0xFF),
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readData(short tag) throws IOException {
			final byte[] cmd = { (byte) 0x80, // CLA Class
					(byte) 0xCA, // INS Instruction
					(byte) ((tag >> 8) & 0xFF), // P1 Parameter 1
					(byte) (tag & 0x1F), // P2 Parameter 2
					(byte) 0x00, // Lc
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response selectByID(byte... id) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(id.length + 6);
			buff.put((byte) 0x00) // CLA Class
					.put((byte) 0xA4) // INS Instruction
					.put((byte) 0x00) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) id.length) // Lc
					.put(id).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}

		public Response selectByName(byte... name) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(name.length + 6);
			buff.put((byte) 0x00) // CLA Class
					.put((byte) 0xA4) // INS Instruction
					.put((byte) 0x04) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) name.length) // Lc
					.put(name).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}

		public Response getProcessingOptions(byte... pdol) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(pdol.length + 6);
			buff.put((byte) 0x80) // CLA Class
					.put((byte) 0xA8) // INS Instruction
					.put((byte) 0x00) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) pdol.length) // Lc
					.put(pdol).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}

		public void connect() throws IOException {
			nfcTag.connect();
		}

		public void close() throws IOException {
			nfcTag.close();
		}

		public byte[] transceive(final byte[] cmd) throws IOException {
			try {
				byte[] rsp = null;

				byte c[] = cmd;
				do {
					byte[] r = nfcTag.transceive(c);
					if (r == null)
						break;

					int N = r.length - 2;
					if (N < 0) {
						rsp = r;
						break;
					}

					if (r[N] == CH_STA_LE) {
						c[c.length - 1] = r[N + 1];
						continue;
					}

					if (rsp == null) {
						rsp = r;
					} else {
						int n = rsp.length;
						N += n;

						rsp = Arrays.copyOf(rsp, N);

						n -= 2;
						for (byte i : r)
							rsp[n++] = i;
					}

					if (r[N] != CH_STA_MORE)
						break;

					byte s = r[N + 1];
					if (s != 0) {
						c = CMD_GETRESPONSE.clone();
					} else {
						rsp[rsp.length - 1] = CH_STA_OK;
						break;
					}

				} while (true);

				return rsp;

			} catch (Exception e) {
				return Response.ERROR;
			}
		}

		private static final byte CH_STA_OK = (byte) 0x90;
		private static final byte CH_STA_MORE = (byte) 0x61;
		private static final byte CH_STA_LE = (byte) 0x6C;
		private static final byte CMD_GETRESPONSE[] = { 0, (byte) 0xC0, 0, 0,
				0, };
	}

	public static final short SW_NO_ERROR = (short) 0x9000;
	public static final short SW_DESFIRE_NO_ERROR = (short) 0x9100;
	public static final short SW_BYTES_REMAINING_00 = 0x6100;
	public static final short SW_WRONG_LENGTH = 0x6700;
	public static final short SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
	public static final short SW_FILE_INVALID = 0x6983;
	public static final short SW_DATA_INVALID = 0x6984;
	public static final short SW_CONDITIONS_NOT_SATISFIED = 0x6985;
	public static final short SW_COMMAND_NOT_ALLOWED = 0x6986;
	public static final short SW_APPLET_SELECT_FAILED = 0x6999;
	public static final short SW_WRONG_DATA = 0x6A80;
	public static final short SW_FUNC_NOT_SUPPORTED = 0x6A81;
	public static final short SW_FILE_NOT_FOUND = 0x6A82;
	public static final short SW_RECORD_NOT_FOUND = 0x6A83;
	public static final short SW_INCORRECT_P1P2 = 0x6A86;
	public static final short SW_WRONG_P1P2 = 0x6B00;
	public static final short SW_CORRECT_LENGTH_00 = 0x6C00;
	public static final short SW_INS_NOT_SUPPORTED = 0x6D00;
	public static final short SW_CLA_NOT_SUPPORTED = 0x6E00;
	public static final short SW_UNKNOWN = 0x6F00;
	public static final short SW_FILE_FULL = 0x6A84;
}
