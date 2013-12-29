package com.dhcc.pos.packets;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dhcc.pos.packets.parse.cnFieldParseInfo;
import com.dhcc.pos.packets.util.ConvertUtil;

/**
 * 消息工厂
 */
public class CnMessageFactory {

	private static CnMessageFactory instance = new CnMessageFactory();

	private CnMessageFactory() {
	}

	public static CnMessageFactory getInstance() {

		return instance;

	}

	/**
	 * 存放用来解析的字段，格式:((msgtypeid, (fieldID, fieldInfo)) msgtypeid 消息类型 cnMessage
	 * 消息 fieldInfo 字段信息
	 */
	private Map<String, Map<Integer, cnFieldParseInfo>> parseMap = new TreeMap<String, Map<Integer, cnFieldParseInfo>>();

	/**
	 * 字段出现的消息中的顺序，格式 ：( msgtypeid, fieldID) msgtypeid 消息类型 fieldID 字段id
	 */
	private Map<String, List<Integer>> parseOrder = new HashMap<String, List<Integer>>();

	/**
	 * 通过字节数组来创建消息
	 * 
	 * @param buf
	 *            8583 消息字节数组
	 * @return
	 */
	public CnMessage parseMessage(byte[] respMsg) throws ParseException {
		CnMessage m = null;
		byte[] msgTypeByte = new byte[2];
		/* 消息类型 */
		String msgType = null;

		/* 位图起止位置 */
		int bitmapStart = -1;
		/* 位图结束位置 */
		int bitmapEnd = -1;
		/**
		 * 拿到消息类型
		 * */
		System.arraycopy(respMsg, 11, msgTypeByte, 0, 2);

		msgType = ConvertUtil._bcd2Str(msgTypeByte);

		/**
		 * 
		 * */
		m = new CnMessage(msgType);
		// TODO it only parses ASCII messages for now

		/**
		 * 位图
		 * */
		byte[] bitmap = new byte[8];

		System.arraycopy(respMsg, 5 + 6 + msgTypeByte.length, bitmap, 0, bitmap.length);
		System.out.println("接收到的位图：\r" + ConvertUtil.trace(bitmap));

		/** 位图以base64形式显示 */

		String bitmapStr = ConvertUtil.bytesToHexString(bitmap);
		System.out.println("bitmap:" + bitmapStr);

		// Parse the bitmap (primary first)
		BitSet bs = new BitSet(64);
		int pos = 0;

		bitmapStart = 5 + 6 + msgTypeByte.length;
		bitmapEnd = 5 + 6 + msgTypeByte.length + 8;

		for (int i = bitmapStart; i < bitmapEnd; i++) {
			int bit = 128;
			for (int b = 0; b < 8; b++) {
				/* (respMsg[i] & bit) != 0 时此域为有效域 */
				bs.set(++pos, (respMsg[i] & bit) != 0);
				/* 右移一位代表初始值128/2的值 */
				bit >>= 1;
			}
		}

		/**
		 * 当bs.get(0)为true时代表有扩展位图此为128位 16个字节；
		 * */
		// Check for secondary bitmap and parse if necessary
		if (bs.get(0)) {
			for (int i = bitmapStart + 8; i < bitmapStart + 8 * 2; i++) {
				int bit = 128;
				for (int b = 0; b < 8; b++) {
					bs.set(++pos, (respMsg[i] & bit) != 0);
					bit >>= 1;
				}
			}
			/* tpdu长+头文件长度+消息类型+位图16（当位图第0位为true时 代表是128位位图，故此占16个字节） */
			pos = bitmapStart + 8 * 2;
			bitmapEnd = bitmapEnd + 8;
		} else {
			/* tpdu长+头文件长度+消息类型+位图8（当位图第0位为false时 代表是64位位图，故此占8个字节） */

			pos = bitmapEnd;

		}

		System.out.println("\t位图: \t" + bs.toString());

		// Parse each field
		Map<Integer, cnFieldParseInfo> parseGuide = parseMap.get(m.getMsgTypeID());
		List<Integer> index = parseOrder.get(m.getMsgTypeID()); // 该类型报文应该存在的域ID集合
		System.out.println("\tindex:\t" + index);
		if (index == null) {
			RuntimeException e = new RuntimeException("在XML文件中未定义报文类型[" + m.getMsgTypeID() + "]的解析配置, 无法解析该类型的报文!! 请完善配置!");
			System.out.println(e.getMessage());

			throw e;
		}

		// 检查2到128，如果收到的报文位图中指示有此域，而XML配置文件中确未指定，则报警告！
		for (int fieldnum = 2; fieldnum <= 128; fieldnum++) {
			if (bs.get(fieldnum)) {
				if (!index.contains(Integer.valueOf(fieldnum))) { // 不包含时
					System.out.println("收到类型为[" + m.getMsgTypeID() + "]的报文中的位图指示含有第[" + fieldnum + "]域,但XML配置文件中未配置该域. 这可能会导致解析错误,建议检查或完善XML配置文件！");
				}
			}
		}
		for (Integer i : index) {
			cnFieldParseInfo fpi = parseGuide.get(i);
			String value62 = null;
			if (bs.get(i)) {
				cnValue val = fpi.parseBinary(respMsg, pos, i);

				m.setField(i, val);
				if (i == 62) {
					value62 = String.valueOf(m.getField(i).getValue());
					m.setValue(i, value62, IFieldType.LLLVAR, value62.length());
				}
				
				System.out.println("\tField=【" + i + "】\t< " + m.getField(i).getType() + "  >\t^" + pos + "^\t(" + m.getField(i).getLength() + ")\t[" + m.getField(i).toString() + "]\t--->\t[" + m.getObjectValue(i) + "]");

				if (!(val.getType().equals(IFieldType.ALPHA) || val.getType().equals(IFieldType.LLVAR) || val.getType().equals(IFieldType.LLLVAR))) {
					pos += (val.getLength() / 2) + (val.getLength() % 2);
				} else {
					pos += val.getLength();
				}
				
				if (val.getType().equals(IFieldType.LLVAR)) {
					pos += 1;
				} else if (val.getType().equals(IFieldType.LLLVAR)) {
					pos += 2;
				} else if (val.getType().equals(IFieldType.LLNVAR)) {
					pos += 1;
				} else if (val.getType().equals(IFieldType.LLLNVAR)) {
					pos += 2;
				}
			}
		}
		return m;
	}

	public void setParseMap(String msgtypeid, Map<Integer, cnFieldParseInfo> map) {
		parseMap.put(msgtypeid, map);
		ArrayList<Integer> index = new ArrayList<Integer>();
		index.addAll(map.keySet());

		/* 升序排序 */
		Collections.sort(index);

		System.out.println("Adding parse map for type: [" + msgtypeid + "] with fields " + index);

		/* 升序排序的域赋给parseOrder */
		parseOrder.put(msgtypeid, index);
	}

	public Map<Integer, cnFieldParseInfo> getParseMap(String msgtypeid) {
		return parseMap.get(msgtypeid);
	}

}
