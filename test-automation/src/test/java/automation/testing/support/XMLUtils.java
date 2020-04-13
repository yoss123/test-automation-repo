package automation.testing.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLUtils {
	
	public static Document getDocumentElement(File xmlFile) 
			throws ParserConfigurationException, FileNotFoundException, SAXException, IOException {
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	    return docBuilder.parse(new FileInputStream(xmlFile));
	}

}
