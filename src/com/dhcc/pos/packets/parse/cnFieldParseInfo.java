package com.dhcc.pos.packets.parse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import com.dhcc.pos.packets.IFieldType;
import com.dhcc.pos.packets.cnValue;
import com.dhcc.pos.packets.util.ConvertUtil;

/***
 * 解析字段域信息
 * 
 * @author maple 参考：此类中参数为配置文件中的<field id="3" datatype="NUMERIC" length="6" />
 */
public class cnFieldParseInfo {

	// 消息类型
	private String type;
	// 消息长度
	private int length;
	// 是否必输项
	private boolean isOk;

	/**
	 * 创建字段解析器
	 * 
	 * @param t
	 *            字段类型
	 * @param len
	 *            字段类型为ALPHA或NUMERIC必须设置此长度
	 * 
	 */
	public cnFieldParseInfo(String type, int len, boolean isOk) {
		this.type = type;
		this.length = len;
		this.isOk = isOk;
	}

	public int getLength() {
		return length;
	}

	public String getType() {
		return type;
	}

	public boolean getIsOk() {
		return isOk;
	}

	/**
	 * 解析 二进制字段信息
	 * 
	 * @param buf
	 *            字节数组报文
	 * @param pos
	 *            该buf中第x位
	 * @param field
	 *            字段
	 * @return
	 * @throws ParseException
	 */
	public cnValue<?> parseBinary(byte[] buf, int pos, int field) throws ParseException {
		/* 字节类型的变量的长度 */
		byte[] fieldLength = null;
		/* 转换后变量的长度 */
		String len = null;
		/* 转换后的域值 */
		String value = null;

		ConvertUtil.trace(buf);

		/* 左靠齐右补零的数据再次删掉右补的零 */
		if (field == 2 || field == 22 || field == 32 || field == 35 || field == 36 || field == 48 || field == 60 || field == 61) {

			if (type.equals(IFieldType.NUMERIC)) {
				byte[] temp = new byte[(length % 2 == 0) ? (length / 2) : (length / 2 + length % 2)];

				System.arraycopy(buf, pos, temp, 0, (length % 2 == 0) ? (length / 2) : (length / 2 + length % 2));
				value = ConvertUtil.Bcd2Str_0(temp);

				return new cnValue<Number>(IFieldType.NUMERIC, new BigInteger(new String(value)), length);

			} else if (type.equals(IFieldType.LLNVAR)) {
				fieldLength = new byte[1];
				System.arraycopy(buf, pos, fieldLength, 0, 1);
				len = ConvertUtil.Bcd2Str(fieldLength);

				length = Integer.parseInt(len);

				System.out.println(ConvertUtil.trace(fieldLength));
				byte[] data = null;
				length = length / 2 + length % 2;
				data = new byte[length];

				System.arraycopy(buf, pos + 1, data, 0, length);
				if (Integer.parseInt(len) % 2 == 0) {
					value = ConvertUtil.Bcd2Str(data);
				} else {
					value = ConvertUtil.Bcd2Str_0(data);
				}

				return new cnValue<String>(type, value, Integer.parseInt(len));

			} else if (type.equals(IFieldType.LLLNVAR)) {
				fieldLength = new byte[2];

				System.arraycopy(buf, pos, fieldLength, 0, 2);
				len = ConvertUtil.Bcd2Str(fieldLength);

				length = Integer.parseInt(len);

				System.out.println(ConvertUtil.trace(fieldLength));

				/* 当长度为奇数时 */
				if (length % 2 == 1) {
					length = length / 2 + length % 2;
					value = new String(buf, pos + 2, length);
					value = ConvertUtil.Bcd2Str_0(value.getBytes());
					return new cnValue<String>(type, value, Integer.parseInt(len));
				} else {
					length = length / 2;
					value = new String(buf, pos + 2, length);
					value = ConvertUtil.Bcd2Str(value.getBytes());
					return new cnValue<String>(type, value, Integer.parseInt(len));
				}
			}

		} else {
			if (type.equals(IFieldType.ALPHA)) {
				return new cnValue<String>(type, new String(buf, pos, length), length);
			} else if (type.equals(IFieldType.NUMERIC)) {
				byte[] temp = new byte[(length % 2 == 0) ? (length / 2) : (length / 2 + length % 2)];

				System.arraycopy(buf, pos, temp, 0, (length % 2 == 0) ? (length / 2) : (length / 2 + length % 2));
				if (length % 2 == 0) {
					value = ConvertUtil.Bcd2Str(temp);
				} else {
					value = ConvertUtil._0Bcd2Str(temp);
				}

				return new cnValue<Number>(IFieldType.NUMERIC, new BigInteger(new String(value)), length);

			} else if (type.equals(IFieldType.LLVAR)) {
				// length = (((buf[pos] & 0xf0) >> 4) * 10) + (buf[pos] & 0x0f);
				// String value = new String(buf, pos + 1,length);

				fieldLength = new byte[1];

				System.arraycopy(buf, pos, fieldLength, 0, 1);
				len = ConvertUtil.Bcd2Str(fieldLength);
				System.out.println(ConvertUtil.trace(fieldLength));

				length = Integer.parseInt(len);
				// System.out.println(String.format("pos: %d, length: %d", pos,
				// length));

				return new cnValue<String>(type, new String(buf, pos + 1, length), length);

			} else if (type.equals(IFieldType.LLLVAR)) {
				fieldLength = new byte[2];

				System.arraycopy(buf, pos, fieldLength, 0, 2);
				len = ConvertUtil.Bcd2Str(fieldLength);

				System.out.println(ConvertUtil.trace(fieldLength));

				length = Integer.parseInt(len);

				// System.out.println(String.format("pos: %d, length: %d", pos,
				// length));
				byte[] data = new byte[length];

				if (field == 62) {
					System.arraycopy(buf, pos + 2, data, 0, length);
					value = ConvertUtil.bytesToHexString(data);
				} else {
					value = new String(buf, pos + 2, length);
				}

				return new cnValue<String>(type, value, length);

			} else if (type.equals(IFieldType.LLNVAR)) {
				fieldLength = new byte[1];
				System.arraycopy(buf, pos, fieldLength, 0, 1);
				len = ConvertUtil.Bcd2Str(fieldLength);

				length = Integer.parseInt(len);
				// System.out.println(String.format("pos: %d, length: %d", pos,
				// length));
				if (length % 2 == 1) {
					length = length / 2 + length % 2;
					byte[] value2 = new byte[length];
					System.arraycopy(buf, pos + 1, value2, 0, length);
					value = ConvertUtil._0Bcd2Str(value2);
					return new cnValue<String>(type, value, Integer.parseInt(len));
				} else {
					length = length / 2;
					byte[] value2 = new byte[length];
					System.arraycopy(buf, pos + 1, value2, 0, length);
					value = ConvertUtil.Bcd2Str(value2);

					return new cnValue<String>(type, value, Integer.parseInt(len));
				}
			} else if (type.equals(IFieldType.LLLNVAR)) {
				fieldLength = new byte[2];

				System.arraycopy(buf, pos, fieldLength, 0, 2);
				len = ConvertUtil.Bcd2Str(fieldLength);

				length = Integer.parseInt(len);

				System.out.println(ConvertUtil.trace(fieldLength));

				/* 当长度为奇数时 */
				if (length % 2 == 1) {
					length = length / 2 + length % 2;
					value = new String(buf, pos + 2, length);
					value = ConvertUtil._0Bcd2Str(value.getBytes());
					return new cnValue<String>(type, value, Integer.parseInt(len));
				} else {
					length = length / 2;
					value = new String(buf, pos + 2, length);
					value = ConvertUtil.Bcd2Str(value.getBytes());
					return new cnValue<String>(type, value, Integer.parseInt(len));
				}
			} else if (type.equals(IFieldType.AMOUNT)) {
				byte[] digits = new byte[6];

				System.arraycopy(buf, pos, digits, 0, 6);
				value = ConvertUtil.Bcd2Str(digits);

				return new cnValue<BigDecimal>(type, new BigDecimal(Double.parseDouble(value.toString()) / 100.00), 12);

			} else if (type.equals(IFieldType.DATE10) || type.equals(IFieldType.DATE4) || type.equals(IFieldType.DATE_EXP) || type.equals(IFieldType.TIME)) {

				int[] tens = new int[length / 2 + length % 2];
				int start = 0;
				for (int i = pos; i < pos + tens.length; i++) {
					tens[start++] = (((buf[i] & 0xf0) >> 4) * 10) + (buf[i] & 0x0f);
				}
				Calendar cal = Calendar.getInstance();
				if (type.equals(IFieldType.DATE10)) {
					cal.set(Calendar.MONTH, tens[0] - 1);
					cal.set(Calendar.DATE, tens[1]);
					cal.set(Calendar.HOUR_OF_DAY, tens[2]);
					cal.set(Calendar.MINUTE, tens[3]);
					cal.set(Calendar.SECOND, tens[4]);
					if (cal.getTime().after(new Date())) {
						cal.add(Calendar.YEAR, -1);
					}
					return new cnValue<Date>(type, cal.getTime(), length);

				} else if (type.equals(IFieldType.DATE4)) {
					cal.set(Calendar.HOUR, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					// Set the month in the date
					cal.set(Calendar.MONTH, tens[0] - 1);
					cal.set(Calendar.DATE, tens[1]);
					if (cal.getTime().after(new Date())) {
						cal.add(Calendar.YEAR, -1);
					}
					return new cnValue<Date>(type, cal.getTime(), length);
				} else if (type.equals(IFieldType.DATE_EXP)) {
					cal.set(Calendar.HOUR, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.DATE, 1);
					// Set the month in the date
					cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - (cal.get(Calendar.YEAR) % 100) + tens[0]);
					cal.set(Calendar.MONTH, tens[1] - 1);
					return new cnValue<Date>(type, cal.getTime(), length);
				} else if (type.equals(IFieldType.TIME)) {
					cal.set(Calendar.HOUR_OF_DAY, tens[0]);
					cal.set(Calendar.MINUTE, tens[1]);
					cal.set(Calendar.SECOND, tens[2]);
					return new cnValue<Date>(type, cal.getTime(), length);
				}

			}
		}
		return null;
	}
}
