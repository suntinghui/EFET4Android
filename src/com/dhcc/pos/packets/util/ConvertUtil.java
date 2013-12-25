package com.dhcc.pos.packets.util;


public class ConvertUtil {
	static final byte[] HEX = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	public static final String nLine = "----------------------------------------------------------------------------";


	/** */
	/**
	 * 把字节数组转换成16进制字符串
	 * 
	 * @param bArray
	 * @return
	 */
	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = null;
		String sTemp;

		if (bArray == null || bArray.length <= 0) {
			return null;
		}
		sb = new StringBuffer(bArray.length);
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 把一个字节数组的串格式化成十六进制形式 格式化后的样式如下 <blockquote>
	 * --------------------------------
	 * -------------------------------------------- 000: 00 38 60 00 12 00 00 60
	 * 22 10 00 00 00 08 00 00 .8`...` ".....:016 016: 20 00 00 00 C0 00 10
	 * 16 83 74 30 30 30 32 31 36 ...�. �t000216:032 032: 37 38 31 30 35 33 36
	 * 30 31 37 30 31 31 30 31 35 78105360 17011015:048 048: 39 00 14 00 00 00
	 * 02 00 10 00 9..... . :064
	 * ----------------------------------------------
	 * ------------------------------ </blockquote>
	 * 
	 * @param inBytes
	 *            需要格式化的字节数组
	 * @return 格式化后的串，其内容如上。可以直接输出
	 */
	public static String trace(byte[] inBytes) {
		int i, j = 0;
		/** 每行字节数 */
		byte[] temp = new byte[76];

		bytesSet(temp, ' ');
		StringBuffer strc = new StringBuffer("");
		strc.append(nLine + "\n");
		for (i = 0; i < inBytes.length; i++) {
			if (j == 0) {
				/** 打印出来的前四位 000: */
				System.arraycopy(String.format("%03d: ", i).getBytes(), 0, temp, 0, 5);

				/** 打印出来的后四位 :015 */
				System.arraycopy(String.format(":%03d", i + 16).getBytes(), 0, temp, 72, 4);
			}

			System.arraycopy(String.format("%02X ", inBytes[i]).getBytes(), 0, temp, j * 3 + 5 + (j > 7 ? 1 : 0), 3);
			if (inBytes[i] == 0x00) {
				temp[j + 55 + ((j > 7 ? 1 : 0))] = '.';
			} else {
				temp[j + 55 + ((j > 7 ? 1 : 0))] = inBytes[i];
			}
			j++;
			/** 当j为16时换行，j重置为0 每行显示为16进制的16个字节 */
			if (j == 16) {
				strc.append(new String(temp)).append("\n");
				bytesSet(temp, ' ');
				j = 0;
			}
		}
		if (j != 0) {
			strc.append(new String(temp)).append("\n");
			bytesSet(temp, ' ');
		}
		strc.append(nLine + "\n");
		// System.out.println(strc.toString());
		return strc.toString();
	}

	/**
	 * 把fill的值替换整个inBytes 例如：
	 * 
	 * @param inBytes
	 * @param fill
	 */
	private static void bytesSet(byte[] inBytes, char fill) {
		if (inBytes.length == 0) {
			return;
		}
		for (int i = 0; i < inBytes.length; i++) {
			inBytes[i] = (byte) fill;
		}
	}

	/**
	 * 此函数为原始函数 1个字节转换成2个字符串 2个字节转换为4个字符串
	 * 
	 * @param bytes
	 * @return
	 */
	public static String Bcd2Str(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);

		for (int i = 0; i < bytes.length; i++) {
			temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
			temp.append((byte) (bytes[i] & 0x0f));
		}
		return temp.toString();
	}

	/**
	 * 1个字节转换成2个字符串 2个字节转换为4个字符串 然后删掉右侧零
	 * 
	 * @param bytes
	 * @return
	 */
	public static String Bcd2Str_0(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);

		for (int i = 0; i < bytes.length; i++) {
			temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
			temp.append((byte) (bytes[i] & 0x0f));
		}

		return temp.toString().substring(0, temp.toString().length() - 1);
	}

	/**
	 * 1个字节转换成2个字符串 2个字节转换为4个字符串 然后删掉左侧零
	 * 
	 * @param bytes
	 * @return
	 */
	public static String _0Bcd2Str(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);

		for (int i = 0; i < bytes.length; i++) {
			temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
			temp.append((byte) (bytes[i] & 0x0f));
		}

		return temp.toString().substring(1, temp.toString().length());
	}

	/**
	 * 检查其数据是否能进行BCD
	 * 
	 * @param val
	 *            待检查的数据
	 * @return 都在 0x00 ~ 0x09, 0x30 ~ 0x3F的范围中，则返回true， 否则false
	 */
	private static boolean canbeBCD(byte[] val) {
		for (int i = 0; i < val.length; i++) {
			if (val[i] < 0x00 || val[i] > 0x3F || (val[i] > 0x09 && val[i] < 0x30))
				return false;
		}
		return true;
	}

	/**
	 * 对给定的数据进行BCD装换
	 * 
	 * @param val
	 *            带装换数据，需满足canbeBCD()。
	 * @return
	 */
	public static byte[] byte2BCD(byte[] val) {
		if (val == null) { // 检查参数是否为null
			System.out.println("不能进行BCD, 传入的参数为null");
			return null;
		}
		byte[] ret_val = val;
		if (!canbeBCD(val)) { // 检查参数的内容是否合法
			System.out.println("不能进行BCD, 传入的参数非法：含有 不在[0x00 ~ 0x09], [0x30 ~ 0x3F]的范围中的数据");
			return ret_val;
		}
		if (val.length == 0) // 当参数的内容的长度为0时，不必进行装换
			return null;
		if (val.length % 2 == 0) { // 长度为偶数时
			ret_val = new byte[val.length / 2];
			for (int i = 0; i < ret_val.length; i++) {
				byte temp1 = (byte) (val[i * 2] < 0x30 ? val[i * 2] : val[i * 2] - 0x30);
				byte temp2 = (byte) (val[i * 2 + 1] < 0x30 ? val[i * 2 + 1] : val[i * 2 + 1] - 0x30);
				ret_val[i] = (byte) (((temp1 << 4) & 0xFF) // 前4位
				| ((temp2 & 0x0F) & 0xFF)); // 后4位
			}
		} else { // 长度为奇数时
			ret_val = new byte[val.length / 2 + 1];
			ret_val[0] = (byte) (val[0] & 0x0F);
			for (int i = 1; i < ret_val.length; i++) {
				byte temp1 = (byte) (val[i * 2 - 1] < 0x30 ? val[i * 2 - 1] : val[i * 2 - 1] - 0x30);
				byte temp2 = (byte) (val[i * 2] < 0x30 ? val[i * 2] : val[i * 2] - 0x30);
				ret_val[i] = (byte) (((temp1 << 4) & 0xFF) // 前4位
				| ((temp2 & 0x0F) & 0xFF)); // 后4位
			}
		}
		return ret_val;
	}

	/**
	 * 右靠齐 左补0
	 * */
	public static String _bcd2Str(byte[] bytes) {
		char temp[] = new char[bytes.length * 2], val;

		for (int i = 0; i < bytes.length; i++) {
			val = (char) (((bytes[i] & 0xf0) >> 4) & 0x0f);
			temp[i * 2] = (char) (val > 9 ? val + 'A' - 10 : val + '0');

			val = (char) (bytes[i] & 0x0f);
			temp[i * 2 + 1] = (char) (val > 9 ? val + 'A' - 10 : val + '0');
		}
		return new String(temp);
	}

	/**
	 * 左靠齐 右补0
	 * 
	 * @函数功能: 10进制串转为BCD码
	 * @输入参数: 10进制串
	 * @输出结果: BCD码
	 */
	public static byte[] str2Bcd_(String asc) {
		int len = asc.length();
		int mod = len % 2;

		if (mod != 0) {
			asc = asc + "0";
			len = asc.length();
		}

		byte abt[] = new byte[len];
		if (len >= 2) {
			len = len / 2;
		}

		byte bbt[] = new byte[len];
		abt = asc.getBytes();
		int j, k;

		for (int p = 0; p < asc.length() / 2; p++) {
			if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
				j = abt[2 * p] - '0';
			} else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
				j = abt[2 * p] - 'a' + 0x0a;
			} else {
				j = abt[2 * p] - 'A' + 0x0a;
			}

			if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
				k = abt[2 * p + 1] - '0';
			} else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
				k = abt[2 * p + 1] - 'a' + 0x0a;
			} else {
				k = abt[2 * p + 1] - 'A' + 0x0a;
			}

			int a = (j << 4) + k;
			byte b = (byte) a;
			bbt[p] = b;
		}
		return bbt;
	}

	/**
	 * 当arc为单数时左补零右靠起，为复数时为原值
	 * 
	 * @函数功能: 10进制串转为BCD码
	 * @输入参数: 10进制串
	 * @输出结果: BCD码
	 */
	public static byte[] _str2Bcd(String arg) {
		int len = arg.length();
		int mod = len % 2;

		if (mod != 0) {
			arg = "0" + arg;
			len = arg.length();
		}

		byte abt[] = new byte[len];
		if (len >= 2) {
			len = len / 2;
		}

		byte bbt[] = new byte[len];
		abt = arg.getBytes();
		int j, k;

		for (int p = 0; p < arg.length() / 2; p++) {
			if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
				j = abt[2 * p] - '0';
			} else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
				j = abt[2 * p] - 'a' + 0x0a;
			} else {
				j = abt[2 * p] - 'A' + 0x0a;
			}

			if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
				k = abt[2 * p + 1] - '0';
			} else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
				k = abt[2 * p + 1] - 'a' + 0x0a;
			} else {
				k = abt[2 * p + 1] - 'A' + 0x0a;
			}

			int a = (j << 4) + k;
			byte b = (byte) a;
			bbt[p] = b;
		}
		return bbt;
	}
}
