/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.config.mapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Escapes configuration values that are rendered into XML configuration files, so that values containing XML
 * significant characters do not produce malformed XML.
 */
final class XmlValueEscaper {

    private static final Log log = LogFactory.getLog(XmlValueEscaper.class);
    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";
    private static final Pattern XML_REFERENCE =
            Pattern.compile("&(?:amp|lt|gt|quot|apos|#[0-9]+|#x[0-9a-fA-F]+);");

    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    static final SAXParserFactory PARSER_FACTORY = createParserFactory(null);

    private XmlValueEscaper() {
    }

    /**
     * Creates a SAX parser factory that never loads external entities or DTDs.
     *
     * @param factoryClassName parser factory implementation to use, or null for the default implementation.
     * @return the parser factory, or null if a parser with these features cannot be created.
     */
    static SAXParserFactory createParserFactory(String factoryClassName) {

        try {
            SAXParserFactory factory = factoryClassName == null ? SAXParserFactory.newInstance() :
                    SAXParserFactory.newInstance(factoryClassName, XmlValueEscaper.class.getClassLoader());
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(LOAD_EXTERNAL_DTD, false);
            factory.newSAXParser();
            return factory;
        } catch (ParserConfigurationException | SAXException | FactoryConfigurationError e) {
            log.warn("Unable to create a secure XML parser. Values rendered into XML configuration files will not "
                    + "be escaped", e);
            return null;
        }
    }

    /**
     * Checks whether the given content is well-formed XML.
     *
     * @param parserFactory parser factory created by {@link #createParserFactory(String)}.
     * @param xml           content to check.
     * @return true if the content is well-formed XML.
     */
    static boolean isWellFormed(SAXParserFactory parserFactory, String xml) {

        try {
            parserFactory.newSAXParser().parse(new InputSource(new StringReader(xml)), new DefaultHandler());
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
    }

    /**
     * Returns a deep copy of the given template context with all string values XML escaped.
     *
     * @param context template context.
     * @return escaped copy of the context.
     */
    static Map<String, Object> escapeContext(Map<String, Object> context) {

        Map<String, Object> escapedContext = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            escapedContext.put(entry.getKey(), escapeObject(entry.getValue()));
        }
        return escapedContext;
    }

    private static Object escapeObject(Object value) {

        if (value instanceof String) {
            return escape((String) value);
        } else if (value instanceof Map) {
            return escapeContext((Map<String, Object>) value);
        } else if (value instanceof List) {
            List<Object> escapedList = new ArrayList<>(((List) value).size());
            for (Object item : (List) value) {
                escapedList.add(escapeObject(item));
            }
            return escapedList;
        }
        return value;
    }

    /**
     * Escapes XML significant characters in a value. Existing character or entity references and CDATA sections are
     * kept as they are, so already escaped values are not escaped again.
     *
     * @param value value to escape.
     * @return escaped value.
     */
    static String escape(String value) {

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        Matcher referenceMatcher = XML_REFERENCE.matcher(value);
        int i = 0;
        while (i < value.length()) {
            if (value.startsWith(CDATA_START, i)) {
                int cdataEnd = value.indexOf(CDATA_END, i + CDATA_START.length());
                if (cdataEnd != -1) {
                    cdataEnd += CDATA_END.length();
                    escaped.append(value, i, cdataEnd);
                    i = cdataEnd;
                    continue;
                }
            }
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    referenceMatcher.region(i, value.length());
                    boolean isReference = referenceMatcher.lookingAt() && isValidReference(referenceMatcher.group());
                    escaped.append(isReference ? "&" : "&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    escaped.append(c);
            }
            i++;
        }
        return escaped.toString();
    }

    /**
     * Checks whether a character reference refers to a character that is allowed in XML.
     */
    private static boolean isValidReference(String reference) {

        if (!reference.startsWith("&#")) {
            return true;
        }
        int codePoint;
        try {
            codePoint = reference.startsWith("&#x") ?
                    Integer.parseInt(reference.substring(3, reference.length() - 1), 16) :
                    Integer.parseInt(reference.substring(2, reference.length() - 1));
        } catch (NumberFormatException e) {
            return false;
        }
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD) || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
