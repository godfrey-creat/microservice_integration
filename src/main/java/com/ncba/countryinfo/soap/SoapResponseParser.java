package com.ncba.countryinfo.soap;

import com.ncba.countryinfo.exception.SoapServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Parses SOAP responses by element local name, so it does not depend on namespace
 * prefixes (m:, soap:). DTDs and external entities are disabled to prevent XXE attacks.
 */
public final class SoapResponseParser {

    private SoapResponseParser() {
    }

    /** Returns the text of CountryISOCodeResult, e.g. "KE". */
    public static String parseCountryIsoCode(String xml) {
        Document doc = parse(xml);
        failOnSoapFault(doc);
        Element result = firstElement(doc, "CountryISOCodeResult");
        if (result == null) {
            throw new SoapServiceException("CountryISOCodeResult element missing from SOAP response");
        }
        return result.getTextContent().trim();
    }

    /** Best-effort extraction of a SOAP Fault's faultstring, or null if there is none. */
    public static String faultStringOrNull(String xml) {
        try {
            Element fault = firstElement(parse(xml), "Fault");
            return fault == null ? null : childText(fault, "faultstring");
        } catch (SoapServiceException e) {
            return null;
        }
    }

    static Document parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new SoapServiceException("Empty SOAP response");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new SoapServiceException("Unreadable SOAP response", e);
        }
    }

    static void failOnSoapFault(Document doc) {
        Element fault = firstElement(doc, "Fault");
        if (fault != null) {
            String faultString = childText(fault, "faultstring");
            throw new SoapServiceException("SOAP Fault: " + (faultString == null ? "unknown" : faultString));
        }
    }

    static Element firstElement(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    /** Text of the first DIRECT child with the given local name. */
    static String childText(Element parent, String localName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }
}
