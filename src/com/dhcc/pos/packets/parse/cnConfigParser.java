package com.dhcc.pos.packets.parse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dhcc.pos.packets.CnMessage;
import com.dhcc.pos.packets.CnMessageFactory;

/**
 * 配置文件解析器
 * 
 * @author maple
 * 
 */
public class cnConfigParser {

	/**
	 * 通过xml文件创建消息工厂，
	 * 
	 * @param filepath
	 *            xml 文件完整路径
	 * @return
	 * @throws Exception
	 */
	public static void createFromXMLConfigFile(InputStream stream) throws Exception {
		try {

			if (stream != null) {
				try {
					// 解析
					parse(stream);
				} finally {
					try {
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 解析xml文件并初始化相关配置信息
	 * 
	 * @param mfact
	 * @param stream
	 * @throws IOException
	 */
	protected static void parse(InputStream stream) throws IOException {
		final DocumentBuilderFactory docfact = DocumentBuilderFactory.newInstance();
		/**
		 * 变量
		 * */
		DocumentBuilder docb = null;
		Document doc = null;
		NodeList nodes = null;
		Element root, elem = null;

		try {
			docb = docfact.newDocumentBuilder();
			doc = docb.parse(stream);
		} catch (ParserConfigurationException ex) {
			System.out.println("Cannot parse XML configuration:" + ex);
			return;
		} catch (SAXException ex) {
			System.out.println("Parsing XML configuration：" + ex);
			return;
		}
		root = doc.getDocumentElement();

		// Read the parsing guides
		nodes = root.getElementsByTagName("parseinfo");
		for (int i = 0; i < nodes.getLength(); i++) {
			elem = (Element) nodes.item(i);
			String msgtypeid = elem.getAttribute("msgtypeid");

			if (msgtypeid.length() != 4) {
				throw new IOException("Invalid type for parse guide: " + msgtypeid);
			}
			
			NodeList fields = elem.getElementsByTagName("field");

			/*
			 * new cnFieldParseInfo(datatype, length) 给解析的字段赋值 以fieldid作为key
			 * cnFieldParseInfo内值为value
			 */
			Map<Integer, cnFieldParseInfo> parseMap = new TreeMap<Integer, cnFieldParseInfo>();

			for (int j = 0; j < fields.getLength(); j++) {
				Element f = (Element) fields.item(j);
				int fieldid = Integer.parseInt(f.getAttribute("id"));

				String datatype = "";
				if (f.getAttribute("datatype") != null){
					datatype = f.getAttribute("datatype");
				}
				
				int length = 0;
				if (f.getAttribute("length").length() > 0) {
					length = Integer.parseInt(f.getAttribute("length"));
				}

				boolean isOk = false;
				if (f.getAttribute("isOk") != null && !(f.getAttribute("isOk").equalsIgnoreCase("false"))) {
					isOk = Boolean.parseBoolean(f.getAttribute("isOk"));
				}

				/*
				 * new cnFieldParseInfo(datatype, length) 给解析的字段赋值
				 * 然后以fieldid作为key 把上面内容放入parseMap中
				 */
				parseMap.put(fieldid, new cnFieldParseInfo(datatype, length, isOk));

			}
			/*
			 * 以msgtypeid作为key 将parseMap放入（CnMessageFactory）的ParseMap之中
			 */
			CnMessageFactory.getInstance().setParseMap(msgtypeid, parseMap);
		}

	}

}
