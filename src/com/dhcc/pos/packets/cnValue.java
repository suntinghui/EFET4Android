package com.dhcc.pos.packets;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dhcc.pos.packets.util.ConvertUtil;

/**
 * 字段域所存储的数据
 * 
 * @author maple
 */
public class cnValue<T> {

	/** 数据类型 */
	private String datatype;

	/** 数值 */
	private T value;

	/** 长度 */
	private int length;

	public cnValue(String type, T val, int len) {
		this.datatype = type;
		this.value = val;
		this.length = len;

		// 设置变长域的长度
//		int tempLen = val.toString().getBytes().length;

		if ((this.datatype.equals(IFieldType.LLVAR)) || (this.datatype.equals(IFieldType.LLNVAR))) {
			if (len > 99) {
				throw new IllegalArgumentException("LLVAR or LLNVAR can only hold values up to 99 chars");
			}
		} else if ((this.datatype.equals(IFieldType.LLLVAR)) || (this.datatype.equals(IFieldType.LLLNVAR))) {
			if (len > 999) {
				throw new IllegalArgumentException("LLLVAR or LLLNVAR can only hold values up to 999 chars");
			}
		}
	}

	public String getType() {
		return this.datatype;
	}

	public int getLength() {
		return this.length;
	}

	public T getValue() {
		return this.value;
	}

	/**
	 * Returns the formatted value as a String. The formatting depends on the
	 * type of the receiver.
	 */
	public String toString() {
		if (value == null) {
			return "ISOValue<null>";
		}

		if (this.datatype.equals(IFieldType.NUMERIC) || this.datatype.equals(IFieldType.AMOUNT)) {
			if (this.datatype.equals(IFieldType.AMOUNT)) {
				return this.format((BigDecimal) value, 12);

			} else if (value instanceof Number) {
				return this.format(((Number) value).longValue(), length);

			} else {
				return this.format(value.toString(), length);
			}

		} else if (this.datatype.equals(IFieldType.ALPHA)) {
			return this.format(value.toString(), length);

		} else if (this.datatype.equals(IFieldType.LLVAR) || this.datatype.equals(IFieldType.LLLVAR)) {
			return value.toString();

		} else if (this.datatype.equals(IFieldType.LLNVAR) || this.datatype.equals(IFieldType.LLLNVAR)) {
			return value.toString();

		} else if (value instanceof Date) {
			return this.format((Date) value);
		}
		return value.toString();
	}

	public cnValue<T> clone() {
		return new cnValue(this.datatype, this.value, this.length);
	}

	public boolean equals(Object other) {
		if ((other == null) || !(other instanceof cnValue)) {
			return false;
		}
		cnValue comp = (cnValue) other;
		return (comp.getType() == getType()) && (comp.getValue().equals(getValue())) && (comp.getLength() == getLength());
	}

	public void write(OutputStream outs, Object fieldId) throws IOException {

		byte[] buf = (byte[]) null;

		if (this.datatype.equals(IFieldType.NUMERIC) || this.datatype.equals(IFieldType.LLNVAR) || this.datatype.equals(IFieldType.LLLNVAR)) {
			buf = new byte[this.length / 2 + this.length % 2];

		} else if (this.datatype.equals(IFieldType.AMOUNT)) {
			buf = new byte[6];

		} else if ((this.datatype.equals(IFieldType.DATE10)) || (this.datatype.equals(IFieldType.DATE4)) || (this.datatype.equals(IFieldType.DATE_EXP)) || (this.datatype.equals(IFieldType.TIME))) {
			buf = new byte[this.length / 2];
		}

		if (buf != null) {
			/* 进行BCD码压缩 */
			buf = toBcd(toString(), buf.length, fieldId);

			outs.write(buf);
			/**
			 * 如果buf不为null证明value被bcd码压缩后并写入到outs中，故此return跳出该函数，
			 * 否则下面的write会重复性再写入一遍该值（不经过bcd码压缩的值）
			 */
			return;
		} else {
			outs.write(toString().getBytes());
		}
	}

	// 日期格式化
	public String format(Date value) {
		if (this.datatype.equals(IFieldType.DATE10))
			return new SimpleDateFormat("MMddHHmmss").format(value);
		if (this.datatype.equals(IFieldType.DATE4))
			return new SimpleDateFormat("MMdd").format(value);
		if (this.datatype.equals(IFieldType.DATE_EXP))
			return new SimpleDateFormat("yyMM").format(value);
		if (this.datatype.equals(IFieldType.TIME)) {
			return new SimpleDateFormat("HHmmss").format(value);
		}
		throw new IllegalArgumentException("Cannot format date as " + this);
	}

	/**
	 * Formats the string to the given length (length is only useful if type is
	 * ALPHA).
	 */
	// 格式化数值
	public String format(String value, int length) {
		if (this.datatype.equals(IFieldType.ALPHA)) {
			if (value == null) {
				value = "";
			}
			if (value.length() > length) {
				return value.substring(0, length);
			}
			// 长度不足右补空格
			char[] c = new char[length];
			System.arraycopy(value.toCharArray(), 0, c, 0, value.length());
			for (int i = value.length(); i < c.length; i++) {
				c[i] = ' ';
			}
			return new String(c);
		} else if (this.datatype.equals(IFieldType.LLVAR) || this.datatype.equals(IFieldType.LLLVAR)) {
			return value;

		} else if (this.datatype.equals(IFieldType.NUMERIC)) {
			char[] c = new char[length];
			char[] x = value.toCharArray();
			if (x.length > length) {
				throw new IllegalArgumentException("Numeric value is larger than intended length: " + value + " LEN " + length);
			}
			// 数字长度不足左补0
			int lim = c.length - x.length;
			for (int i = 0; i < lim; i++) {
				c[i] = '0';
			}
			// arraycopy(Object src, int srcPos,Object dest, int destPos,int
			// length)
			System.arraycopy(x, 0, c, lim, x.length);
			return new String(c);
		}
		throw new IllegalArgumentException("Cannot format String as " + this);
	}

	/** Formats the integer value as a NUMERIC, an AMOUNT, or a String. */
	public String format(long value, int length) {
		if (this.datatype.equals(IFieldType.NUMERIC)) {
			char[] c = new char[length];
			char[] x = Long.toString(value).toCharArray();
			if (x.length > length) {
				throw new IllegalArgumentException("Numeric value is larger than intended length: " + value + " LEN " + length);
			}
			// 数字长度不足左补0
			int lim = c.length - x.length;
			for (int i = 0; i < lim; i++) {
				c[i] = '0';
			}
			System.arraycopy(x, 0, c, lim, x.length);
			return new String(c);
		} else if (this.datatype.equals(IFieldType.ALPHA) || this.datatype.equals(IFieldType.LLVAR) || this.datatype.equals(IFieldType.LLLVAR)) {
			return format(Long.toString(value), length);
		} else if (this.datatype.equals(IFieldType.AMOUNT)) {
			String v = Long.toString(value);
			char[] digits = new char[12];
			// 金额长度不足左补0
			for (int i = 0; i < 12; i++) {
				digits[i] = '0';
			}
			// No hay decimales asi que dejamos los dos ultimos digitos como 0
			System.arraycopy(v.toCharArray(), 0, digits, 10 - v.length(), v.length());
			return new String(digits);
		}
		throw new IllegalArgumentException("Cannot format number as " + this);
	}

	/** Formats the BigDecimal as an AMOUNT, NUMERIC, or a String. */
	// public String format(BigDecimal value, int length) {
	public String format(BigDecimal value, int length) {
		if (this.datatype.equals(IFieldType.AMOUNT)) {
			// 金额格式化
			String v = new DecimalFormat("0000000000.00").format(value);
			return v.substring(0, 10) + v.substring(11);
		} else if (this.datatype.equals(IFieldType.NUMERIC)) {
			return format(value.longValue(), length);
		} else if (this.datatype.equals(IFieldType.ALPHA) || this.datatype.equals(IFieldType.LLVAR) || this.datatype.equals(IFieldType.LLLVAR)) {
			return format(value.toString(), length);
		}
		throw new IllegalArgumentException("Cannot format BigDecimal as " + this);
	}

	/**
	 * @param value
	 * @param buf
	 * @param fieldId
	 */
	private byte[] toBcd(String value, int bufLen, Object fieldId) {
		byte[] buf = new byte[bufLen];
		int field = Integer.valueOf(fieldId.toString());

		/** 当域值为奇数时 需要根据不同域名来指定左靠齐或右靠齐 */
		if (value.length() % 2 == 1) {
			/* 左靠齐右补零 */
			if (field == 2 | field == 22 | field == 35 | field == 60) {
				buf = ConvertUtil.str2Bcd_(value);

			} else {
				buf = ConvertUtil._str2Bcd(value);
			}
		} else {
			buf = ConvertUtil._str2Bcd(value);
		}
		return buf;
	}
}