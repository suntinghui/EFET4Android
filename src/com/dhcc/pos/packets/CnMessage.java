package com.dhcc.pos.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dhcc.pos.packets.util.ConvertUtil;

/**
 * iso 8583 消息信息 报文总长度（2-4字节）+报头信息+报文类型+位图（8（64位图）或16（128位图））+各字段域+结束符
 */
public class CnMessage {
	public String bitMap = null;

	/** 消息类型 */
	private String msgtypeid;

	/** bit map 位图 */
	private Map<Integer, cnValue<?>> fields = new ConcurrentHashMap<Integer, cnValue<?>>();

	public CnMessage() {

	}

	/**
	 * 创建 一个指定类型的8583消息
	 * 
	 * @param msgtypeid
	 *            消息类型
	 * @param msgTPDUlength
	 *            ,msgHeaderlength
	 */
	public CnMessage(String msgtypeid) {
		this.msgtypeid = msgtypeid;
	}

	/** 获取消息类型. */
	public String getMsgTypeID() {
		return msgtypeid;
	}

	/**
	 * 返回字段域数值（ 应该在2-128范围，1字段域用来存放位图）
	 * 
	 * @param fieldid
	 *            字段域id
	 * @return
	 */
	public Object getObjectValue(int fieldid) {
		cnValue<?> v = fields.get(fieldid);
		if (v == null) {
			return null;
		}
		return v.getValue();
	}

	/**
	 * 返回字段域数值（ 应该在2-128范围，1字段域用来存放位图）
	 * 
	 * @param fieldid
	 *            字段域id
	 * @return
	 */
	public cnValue<?> getField(int fieldid) {
		return fields.get(fieldid);
	}

	/**
	 * 设置字段域，由于字段域1被 用来存放位图，设置字段域应从2开始
	 * 
	 * @param fieldid
	 *            字段域id
	 * @param field
	 *            字段数值
	 */
	public void setField(int fieldid, cnValue<?> field) {
		if (fieldid < 2 || fieldid > 128) {
			throw new IndexOutOfBoundsException("Field index must be between 2 and 128");
		}
		if (field == null) {
			fields.remove(fieldid);
		} else {
			fields.put(fieldid, field);
		}
	}

	/**
	 * 设置字段域，由于字段域1被 用来存放位图，设置字段域应从2开始
	 * 
	 * @param fieldid
	 *            字段域id
	 * @param value
	 *            数值
	 * @param t
	 *            类型
	 * @param length
	 *            长度
	 */
	public void setValue(int fieldid, Object value, String type, int length) {
		if (fieldid < 2 || fieldid > 128) {
			throw new IndexOutOfBoundsException("Field index must be between 2 and 128");
		}
		if (value == null) {
			fields.remove(fieldid);
			
		} else {
			cnValue<?> v = new cnValue<Object>(type, value, length);
			fields.put(fieldid, v);
		}
	}

	/**
	 * 是否存在该字段
	 * 
	 * @param fieldid
	 * @return
	 */
	public boolean hasField(int fieldid) {
		return fields.get(fieldid) != null;
	}

	/**
	 * 
	 * 返回报文内容 ,不包含前报文总长度及结束符
	 * 
	 * @return位图[8字节]+ 11域【3字节BCD码】+其余所有域值（个别域值前加上BCD码压缩的2个字节的长度值_左补0）
	 */
	public byte[] writeInternal() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		// Bitmap
		ArrayList<Integer> keys = new ArrayList<Integer>();
		keys.addAll(fields.keySet());
		Collections.sort(keys);
		BitSet bs = new BitSet(64);

		for (Integer i : keys) { // BitSet可以自动扩展大小
			bs.set(i - 1, true);
		}
		// Extend to 128 if needed
		if (bs.length() > 64) {
			BitSet b2 = new BitSet(128);
			b2.or(bs); // 得到位图(根据域的个数，可能自动扩展)
			bs = b2;
			/* 当bs长度大于64时 设定第一位为true */
			bs.set(0, true);
		}
		// Write bitmap into stream
		int pos = 128; // 用来做位运算： -- 1000 0000（初值最高位为1，然后右移一位，等等）
		int b = 0; // 用来做位运算：初值二进制位全0
		for (int i = 0; i < bs.size(); i++) {
			if (bs.get(i)) {
				b |= pos;
			}
			pos >>= 1;

			if (pos == 0) { // 到一个字节时（8位），就写入
				bout.write(b);
				pos = 128;
				b = 0;
			}

		}
		System.out.println("位图长度:\t" + bout.toByteArray().length + "\r十六进制位图：\r" + ConvertUtil.trace(bout.toByteArray()));

		bitMap = ConvertUtil.bytesToHexString(bout.toByteArray());

		System.out.println("bitMap[" + bitMap + "]");

		/**
		 * Fields 紧跟着位图后面 位图所有域的值
		 * */
		for (Integer i : keys) {
			cnValue<?> v = fields.get(i);
			/** 当i不等于该域时 证明该域不需要加长度值 */
			if (i != 52) {
				
				if (v.getType().equals(IFieldType.LLVAR) | v.getType().equals(IFieldType.LLNVAR)) {
					int length = v.getValue().toString().length();

					byte[] byteFieldLLVAR = ConvertUtil.str2Bcd_(String.format("%02d", length));

					try {
						bout.write(byteFieldLLVAR);
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else if (v.getType().equals(IFieldType.LLLVAR) | v.getType().equals(IFieldType.LLLNVAR)) {
					int length = v.getValue().toString().length();

					byte[] byteFieldLLLVAR = ConvertUtil.str2Bcd_(String.format("%04d", length));

					try {
						bout.write(byteFieldLLLVAR);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				v.write(bout, i);
			} catch (IOException ex) {
				// should never happen, writing to a ByteArrayOutputStream
			}
		}
		return bout.toByteArray();
	}

}
