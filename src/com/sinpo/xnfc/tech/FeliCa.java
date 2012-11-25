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

package com.sinpo.xnfc.tech;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.nfc.tech.NfcF;

import com.sinpo.xnfc.Util;

public class FeliCa {
	public static final byte[] EMPTY = {};

	protected byte[] data;

	protected FeliCa() {
	}

	protected FeliCa(byte[] bytes) {
		data = (bytes == null) ? FeliCa.EMPTY : bytes;
	}

	public int size() {
		return data.length;
	}

	public byte[] getBytes() {
		return data;
	}

	@Override
	public String toString() {
		return Util.toHexString(data, 0, data.length);
	}

	public final static class IDm extends FeliCa {
		public static final byte[] EMPTY = { 0, 0, 0, 0, 0, 0, 0, 0, };

		public IDm(byte[] bytes) {
			super((bytes == null || bytes.length < 8) ? IDm.EMPTY : bytes);
		}

		public final String getManufactureCode() {
			return Util.toHexString(data, 0, 2);
		}

		public final String getCardIdentification() {
			return Util.toHexString(data, 2, 6);
		}

		public boolean isEmpty() {
			final byte[] d = data;
			for (final byte v : d) {
				if (v != 0)
					return false;
			}
			return true;
		}
	}

	public final static class PMm extends FeliCa {
		public static final byte[] EMPTY = { 0, 0, 0, 0, 0, 0, 0, 0, };

		public PMm(byte[] bytes) {
			super((bytes == null || bytes.length < 8) ? PMm.EMPTY : bytes);
		}

		public final String getIcCode() {
			return Util.toHexString(data, 0, 2);
		}

		public final String getMaximumResponseTime() {
			return Util.toHexString(data, 2, 6);
		}
	}

	public final static class SystemCode extends FeliCa {
		public static final byte[] EMPTY = { 0, 0, };

		public SystemCode(byte[] sc) {
			super((sc == null || sc.length < 2) ? SystemCode.EMPTY : sc);
		}

		public int toInt() {
			return toInt(data);
		}

		public static int toInt(byte[] data) {
			return 0x0000FFFF & ((data[0] << 8) | (0x000000FF & data[1]));
		}
	}

	public final static class ServiceCode extends FeliCa {
		public static final byte[] EMPTY = { 0, 0, };
		public static final int T_UNKNOWN = 0;
		public static final int T_RANDOM = 1;
		public static final int T_CYCLIC = 2;
		public static final int T_PURSE = 3;

		public ServiceCode(byte[] sc) {
			super((sc == null || sc.length < 2) ? ServiceCode.EMPTY : sc);
		}

		public ServiceCode(int code) {
			this(new byte[] { (byte) (code & 0xFF), (byte) (code >> 8) });
		}

		public boolean isEncrypt() {
			return (data[0] & 0x1) == 0;
		}

		public boolean isWritable() {
			final int f = data[0] & 0x3F;
			return (f & 0x2) == 0 || f == 0x13 || f == 0x12;
		}

		public int getAccessAttr() {
			return data[0] & 0x3F;
		}

		public int getDataType() {
			final int f = data[0] & 0x3F;
			if ((f & 0x10) == 0)
				return T_PURSE;

			return ((f & 0x04) == 0) ? T_RANDOM : T_CYCLIC;
		}
	}

	public final static class Block extends FeliCa {
		public Block() {
			data = new byte[16];
		}

		public Block(byte[] bytes) {
			super((bytes == null || bytes.length < 16) ? new byte[16] : bytes);
		}
	}

	public final static class BlockListElement extends FeliCa {
		private static final byte LENGTH_2_BYTE = (byte) 0x80;
		private static final byte LENGTH_3_BYTE = (byte) 0x00;
		// private static final byte ACCESSMODE_DECREMENT = (byte) 0x00;
		// private static final byte ACCESSMODE_CACHEBACK = (byte) 0x01;
		private final byte lengthAndaccessMode;
		private final byte serviceCodeListOrder;

		public BlockListElement(byte mode, byte order, byte... blockNumber) {
			if (blockNumber.length > 1) {
				lengthAndaccessMode = (byte) (mode | LENGTH_2_BYTE & 0xFF);
			} else {
				lengthAndaccessMode = (byte) (mode | LENGTH_3_BYTE & 0xFF);
			}
			serviceCodeListOrder = (byte) (order & 0x0F);
			data = (blockNumber == null) ? FeliCa.EMPTY : blockNumber;
		}

		@Override
		public byte[] getBytes() {
			if ((this.lengthAndaccessMode & LENGTH_2_BYTE) == 1) {
				ByteBuffer buff = ByteBuffer.allocate(2);
				buff.put(
						(byte) ((this.lengthAndaccessMode | this.serviceCodeListOrder) & 0xFF))
						.put(data[0]);
				return buff.array();
			} else {
				ByteBuffer buff = ByteBuffer.allocate(3);
				buff.put(
						(byte) ((this.lengthAndaccessMode | this.serviceCodeListOrder) & 0xFF))
						.put(data[1]).put(data[0]);
				return buff.array();
			}
		}
	}

	public final static class MemoryConfigurationBlock extends FeliCa {
		public MemoryConfigurationBlock(byte[] bytes) {
			super((bytes == null || bytes.length < 4) ? new byte[4] : bytes);
		}

		public boolean isNdefSupport() {
			return (data == null) ? false : (data[3] & (byte) 0xff) == 1;
		}

		public void setNdefSupport(boolean ndefSupport) {
			data[3] = (byte) (ndefSupport ? 1 : 0);
		}

		public boolean isWritable(int... addrs) {
			if (data == null)
				return false;

			boolean result = true;
			for (int a : addrs) {
				byte b = (byte) ((a & 0xff) + 1);
				if (a < 8) {
					result &= (data[0] & b) == b;
					continue;
				} else if (a < 16) {
					result &= (data[1] & b) == b;
					continue;
				} else
					result &= (data[2] & b) == b;
			}
			return result;
		}
	}

	public final static class Service extends FeliCa {
		private final ServiceCode[] serviceCodes;
		private final BlockListElement[] blockListElements;

		public Service(ServiceCode[] codes, BlockListElement... blocks) {
			serviceCodes = (codes == null) ? new ServiceCode[0] : codes;
			blockListElements = (blocks == null) ? new BlockListElement[0]
					: blocks;
		}

		@Override
		public byte[] getBytes() {

			int length = 0;
			for (ServiceCode s : this.serviceCodes) {
				length += s.getBytes().length;
			}

			for (BlockListElement b : blockListElements) {
				length += b.getBytes().length;
			}

			ByteBuffer buff = ByteBuffer.allocate(length);
			for (ServiceCode s : this.serviceCodes) {
				buff.put(s.getBytes());
			}

			for (BlockListElement b : blockListElements) {
				buff.put(b.getBytes());
			}

			return buff.array();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (ServiceCode s : serviceCodes) {
				sb.append(s.toString());
			}

			for (BlockListElement b : blockListElements) {
				sb.append(b.toString());
			}
			return sb.toString();
		}
	}

	public final static class Command extends FeliCa {
		private final int length;
		private final byte code;
		private final IDm idm;

		public Command(final byte[] bytes) {
			this(bytes[0], Arrays.copyOfRange(bytes, 1, bytes.length));
		}

		public Command(byte code, final byte... bytes) {
			this.code = code;
			if (bytes.length >= 8) {
				idm = new IDm(Arrays.copyOfRange(bytes, 0, 8));
				data = Arrays.copyOfRange(bytes, 8, bytes.length);
			} else {
				idm = null;
				data = bytes;
			}
			length = bytes.length + 2;
		}

		public Command(byte code, IDm idm, final byte... bytes) {
			this.code = code;
			this.idm = idm;
			this.data = bytes;
			this.length = idm.getBytes().length + data.length + 2;
		}

		public Command(byte code, byte[] idm, final byte... bytes) {
			this.code = code;
			this.idm = new IDm(idm);
			this.data = bytes;
			this.length = idm.length + data.length + 2;
		}

		@Override
		public byte[] getBytes() {
			ByteBuffer buff = ByteBuffer.allocate(length);
			byte length = (byte) this.length;
			if (idm != null) {
				buff.put(length).put(code).put(idm.getBytes()).put(data);
			} else {
				buff.put(length).put(code).put(data);
			}
			return buff.array();
		}
	}

	public static class Response extends FeliCa {
		protected final int length;
		protected final byte code;
		protected final IDm idm;

		public Response(byte[] bytes) {
			if (bytes != null && bytes.length >= 10) {
				length = bytes[0] & 0xff;
				code = bytes[1];
				idm = new IDm(Arrays.copyOfRange(bytes, 2, 10));
				data = bytes;
			} else {
				length = 0;
				code = 0;
				idm = new IDm(null);
				data = FeliCa.EMPTY;
			}
		}

		public IDm getIDm() {
			return idm;
		}
	}

	public final static class PollingResponse extends Response {
		private final PMm pmm;

		public PollingResponse(byte[] bytes) {
			super(bytes);
			if (size() >= 18) {
				pmm = new PMm(Arrays.copyOfRange(data, 10, 18));
			} else {
				pmm = new PMm(null);
			}
		}

		public PMm getPMm() {
			return pmm;
		}
	}

	public final static class ReadResponse extends Response {
		public static final byte[] EMPTY = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				(byte) 0xFF, (byte) 0xFF };

		private final byte[] blockData;

		public ReadResponse(byte[] rsp) {
			super((rsp == null || rsp.length < 12) ? ReadResponse.EMPTY : rsp);

			if (getStatusFlag1() == STA1_NORMAL && getBlockCount() > 0) {
				blockData = Arrays.copyOfRange(data, 13, data.length);
			} else {
				blockData = FeliCa.EMPTY;
			}
		}

		public int getStatusFlag1() {
			return data[10];
		}

		public int getStatusFlag2() {
			return data[11];
		}

		public int getBlockCount() {
			return (data.length > 12) ? (0xFF & data[12]) : 0;
		}

		public byte[] getBlockData() {
			return blockData;
		}

		public boolean isOkey() {
			return getStatusFlag1() == STA1_NORMAL;
		}
	}

	public final static class WriteResponse extends Response {
		public WriteResponse(byte[] rsp) {
			super((rsp == null || rsp.length < 12) ? ReadResponse.EMPTY : rsp);
		}

		public int getStatusFlag1() {
			return data[0];
		}

		public int getStatusFlag2() {
			return data[1];
		}

		public boolean isOkey() {
			return getStatusFlag1() == STA1_NORMAL;
		}
	}

	public final static class Tag {
		private final NfcF nfcTag;

		private boolean isFeliCaLite;
		private int sys;
		private IDm idm;
		private PMm pmm;

		public Tag(NfcF tag) {
			nfcTag = tag;
			sys = SystemCode.toInt(tag.getSystemCode());
			idm = new IDm(tag.getTag().getId());
			pmm = new PMm(tag.getManufacturer());
		}

		public int getSystemCode() {
			return sys;
		}

		public IDm getIDm() {
			return idm;
		}

		public PMm getPMm() {
			return pmm;
		}

		public boolean checkFeliCaLite() {
			isFeliCaLite = !polling(SYS_FELICA_LITE).getIDm().isEmpty();
			return isFeliCaLite;
		}

		public boolean isFeliCaLite() {
			return isFeliCaLite;
		}

		public PollingResponse polling(int systemCode) {
			Command cmd = new Command(CMD_POLLING, new byte[] {
					(byte) (systemCode >> 8), (byte) (systemCode & 0xff),
					(byte) 0x01, (byte) 0x00 });

			PollingResponse r = new PollingResponse(transceive(cmd));
			idm = r.getIDm();
			pmm = r.getPMm();
			return r;
		}

		public PollingResponse polling() {
			Command cmd = new Command(CMD_POLLING, new byte[] {
					(byte) (SYS_FELICA_LITE >> 8),
					(byte) (SYS_FELICA_LITE & 0xff), (byte) 0x01, (byte) 0x00 });
			PollingResponse r = new PollingResponse(transceive(cmd));
			idm = r.getIDm();
			pmm = r.getPMm();
			return r;
		}

		public final SystemCode[] getSystemCodeList() {
			final Command cmd = new Command(CMD_REQUEST_SYSTEMCODE, idm);
			final byte[] bytes = transceive(cmd);
			final int num = (int) bytes[10];
			final SystemCode ret[] = new SystemCode[num];

			for (int i = 0; i < num; ++i) {
				ret[i] = new SystemCode(Arrays.copyOfRange(bytes, 11 + i * 2,
						13 + i * 2));
			}
			return ret;
		}

		public ServiceCode[] getServiceCodeList() {
			ArrayList<ServiceCode> ret = new ArrayList<ServiceCode>();

			int index = 1;
			while (true) {
				byte[] bytes = searchServiceCode(index);
				if (bytes.length != 2 && bytes.length != 4)
					break;

				if (bytes.length == 2) {
					if (bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xff)
						break;

					ret.add(new ServiceCode(bytes));
				}
				++index;
			}
			return ret.toArray(new ServiceCode[ret.size()]);
		}

		private byte[] searchServiceCode(int index) {
			Command cmd = new Command(CMD_SEARCH_SERVICECODE, idm, new byte[] {
					(byte) (index & 0xff), (byte) (index >> 8) });

			byte[] bytes = transceive(cmd);
			if (bytes == null || bytes.length < 12 || bytes[1] != (byte) 0x0b) {
				return FeliCa.EMPTY;
			}
			return Arrays.copyOfRange(bytes, 10, bytes.length);
		}

		public ReadResponse readWithoutEncryption(ServiceCode code, byte addr) {
			byte[] bytes = code.getBytes();
			Command cmd = new Command(CMD_READ_WO_ENCRYPTION, idm, new byte[] {
					(byte) 0x01, (byte) bytes[0], (byte) bytes[1], (byte) 0x01,
					(byte) 0x80, addr });
			return new ReadResponse(transceive(cmd));
		}

		public ReadResponse readWithoutEncryption(byte addr) {
			Command cmd = new Command(CMD_READ_WO_ENCRYPTION, idm, new byte[] {
					(byte) 0x01, (byte) (SRV_FELICA_LITE_READONLY >> 8),
					(byte) (SRV_FELICA_LITE_READONLY & 0xff), (byte) 0x01,
					(byte) 0x80, addr });
			return new ReadResponse(transceive(cmd));
		}

		public WriteResponse writeWithoutEncryption(ServiceCode code,
				byte addr, byte[] buff) {

			byte[] bytes = code.getBytes();
			ByteBuffer b = ByteBuffer.allocate(22);
			b.put(new byte[] { (byte) 0x01, (byte) bytes[0], (byte) bytes[1],
					(byte) 0x01, (byte) 0x80, (byte) addr });
			b.put(buff, 0, buff.length > 16 ? 16 : buff.length);
			Command cmd = new Command(CMD_WRITE_WO_ENCRYPTION, idm, b.array());
			return new WriteResponse(transceive(cmd));
		}

		public WriteResponse writeWithoutEncryption(byte addr, byte[] buff) {

			ByteBuffer b = ByteBuffer.allocate(22);
			b.put(new byte[] { (byte) 0x01,
					(byte) (SRV_FELICA_LITE_READWRITE >> 8),
					(byte) (SRV_FELICA_LITE_READWRITE & 0xff), (byte) 0x01,
					(byte) 0x80, addr });
			b.put(buff, 0, buff.length > 16 ? 16 : buff.length);

			Command cmd = new Command(CMD_WRITE_WO_ENCRYPTION, idm, b.array());

			return new WriteResponse(transceive(cmd));
		}

		public MemoryConfigurationBlock getMemoryConfigBlock() {
			ReadResponse r = readWithoutEncryption((byte) 0x88);
			return (r != null) ? new MemoryConfigurationBlock(r.getBlockData())
					: null;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (idm != null) {
				sb.append(idm.toString());

				if (pmm != null)
					sb.append(pmm.toString());
			}
			return sb.toString();
		}

		public void connect() {
			try {
				nfcTag.connect();
			} catch (Exception e) {
			}
		}

		public void close() {
			try {
				nfcTag.close();
			} catch (Exception e) {
			}
		}

		public byte[] transceive(Command cmd) {
			try {
				return nfcTag.transceive(cmd.getBytes());
			} catch (Exception e) {
				return Response.EMPTY;
			}
		}
	}

	// polling
	public static final byte CMD_POLLING = 0x00;
	public static final byte RSP_POLLING = 0x01;

	// request service
	public static final byte CMD_REQUEST_SERVICE = 0x02;
	public static final byte RSP_REQUEST_SERVICE = 0x03;

	// request RESPONSE
	public static final byte CMD_REQUEST_RESPONSE = 0x04;
	public static final byte RSP_REQUEST_RESPONSE = 0x05;

	// read without encryption
	public static final byte CMD_READ_WO_ENCRYPTION = 0x06;
	public static final byte RSP_READ_WO_ENCRYPTION = 0x07;

	// write without encryption
	public static final byte CMD_WRITE_WO_ENCRYPTION = 0x08;
	public static final byte RSP_WRITE_WO_ENCRYPTION = 0x09;

	// search service code
	public static final byte CMD_SEARCH_SERVICECODE = 0x0a;
	public static final byte RSP_SEARCH_SERVICECODE = 0x0b;

	// request system code
	public static final byte CMD_REQUEST_SYSTEMCODE = 0x0c;
	public static final byte RSP_REQUEST_SYSTEMCODE = 0x0d;

	// authentication 1
	public static final byte CMD_AUTHENTICATION1 = 0x10;
	public static final byte RSP_AUTHENTICATION1 = 0x11;

	// authentication 2
	public static final byte CMD_AUTHENTICATION2 = 0x12;
	public static final byte RSP_AUTHENTICATION2 = 0x13;

	// read
	public static final byte CMD_READ = 0x14;
	public static final byte RSP_READ = 0x15;

	// write
	public static final byte CMD_WRITE = 0x16;
	public static final byte RSP_WRITE = 0x17;

	public static final int SYS_ANY = 0xffff;
	public static final int SYS_FELICA_LITE = 0x88b4;
	public static final int SYS_COMMON = 0xfe00;

	public static final int SRV_FELICA_LITE_READONLY = 0x0b00;
	public static final int SRV_FELICA_LITE_READWRITE = 0x0900;

	public static final int STA1_NORMAL = 0x00;
	public static final int STA1_ERROR = 0xff;

	public static final int STA2_NORMAL = 0x00;
	public static final int STA2_ERROR_LENGTH = 0x01;
	public static final int STA2_ERROR_FLOWN = 0x02;
	public static final int STA2_ERROR_MEMORY = 0x70;
	public static final int STA2_ERROR_WRITELIMIT = 0x71;
}
