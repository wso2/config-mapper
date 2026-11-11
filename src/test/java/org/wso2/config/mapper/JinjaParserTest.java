/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import com.hubspot.jinjava.Jinjava;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.config.mapper.model.Context;
import org.xml.sax.SAXNotRecognizedException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Test cases for converting flat dotted keys into hierarchical key model expected by Jinja parser.
 */
public class JinjaParserTest {

    private Map<String, Object> inputMap = new HashMap<>();

    @BeforeMethod
    public void setUp() {
        for (int i = 0; i < flatKeyDataSet().length; i++) {
            inputMap.put((String) flatKeyDataSet()[i][0], flatKeyDataSet()[i][1]);
        }
    }

    @Test(dataProvider = "flatKeySetProvider")
    public void testDottedKeyProcessing(String key, Object value) throws ConfigParserException {
        Map<String, Object> outputMap = JinjaParser.getHierarchicalDottedKeyMap(inputMap);
        String[] dottedKeyArray = key.split("\\.");

        Map<String, Object> lastKeyMap = outputMap;
        for (int i = 0; i < dottedKeyArray.length - 1; i++) {
            Object obj = lastKeyMap.get(dottedKeyArray[i]);
            Assert.assertTrue(obj instanceof Map, "Value should be a map");
            lastKeyMap = (Map) obj;
        }
        Assert.assertEquals(lastKeyMap.get(dottedKeyArray[dottedKeyArray.length - 1]), value);
    }

    @Test (dataProvider = "splitDataProvider")
    public void testSplitMethod(String input, String[] expectedOutput) {
        List<String> stringList = JinjaParser.splitWithoutEmptyStrings(input);

        Assert.assertEquals(stringList.size(), expectedOutput.length, "Split item count mismatch.");
        for (int i = 0; i < stringList.size(); i++) {
            Assert.assertEquals(stringList.get(i), expectedOutput[i], "Split value mismatch.");
        }
    }

    @Test (dataProvider = "dottedKeyArraySplitDataProvider")
    public void testDottedKeyArraySplit(String input, String[] expectedOutput) throws ConfigParserException {
        List<String> stringList = JinjaParser.getDottedKeyArray(input);

        Assert.assertEquals(stringList.size(), expectedOutput.length, "Split item count mismatch.");
        for (int i = 0; i < stringList.size(); i++) {
            Assert.assertEquals(stringList.get(i), expectedOutput[i], "Split value mismatch.");
        }
    }

    @DataProvider(name = "dottedKeyArraySplitDataProvider")
    public Object[][] dottedKeyArraySplitData() {
        return new Object[][]{
                {"one.two.three.four", new String[]{"one", "two", "three", "four"}},
                {".one.'two.three'", new String[]{"one", "two.three"}},
                {".one.'two.three'.four", new String[]{"one", "two.three", "four"}},
                {".one.two.'three.four'.five.six", new String[]{"one", "two", "three.four", "five", "six"}},
                {".one.two.'three.four.five'.six.seven", new String[]{"one", "two", "three.four.five", "six", "seven"}},
                {"one.two.'three.four.five'.six.'seven.eight'", new String[]{"one", "two", "three.four.five", "six",
                        "seven.eight"}},
                {".one.two.'three.four.five'.six.'seven.eight'", new String[]{"one", "two", "three.four.five", "six",
                        "seven.eight"}},
                {"'one.two'.three.four.five.'six.seven'.eight", new String[]{"one.two", "three", "four", "five",
                        "six.seven", "eight"}},
                {".'one.two'.three.four.five.'six.seven'.eight", new String[]{"one.two", "three", "four", "five",
                        "six.seven", "eight"}},
                {"one.'two.three'.four.'five.six'.seven.'eight.nine'", new String[]{"one", "two.three", "four",
                        "five.six", "seven", "eight.nine"}},
                {".one.'two.three'.four.'five.six'.seven.'eight.nine'", new String[]{"one", "two.three", "four",
                        "five.six", "seven", "eight.nine"}},
                {"'one.two'.'three.four'.'five.six'.seven.'eight.nine'", new String[]{"one.two", "three.four",
                        "five.six", "seven", "eight.nine"}},
                {".'one.two'.'three.four'.'five.six'.seven.'eight.nine'", new String[]{"one.two", "three.four",
                        "five.six", "seven", "eight.nine"}},
                };
    }

    @DataProvider(name = "splitDataProvider")
    public Object[][] splitTestData() {
        return new Object[][]{
                {"one.two.three", new String[]{"one", "two", "three"}},
                {".one.two.three.", new String[]{"one", "two", "three"}},
                {".one.two.four.", new String[]{"one", "two", "four"}},
                {".one.two.three..four.", new String[]{"one", "two", "three", "four"}},
                };
    }

    @DataProvider(name = "flatKeySetProvider")
    public Object[][] flatKeyDataSet() {
        return new Object[][]{
                {"a.b.c", "value1"},
                {"a.b.d", "value2"},
                {"e", "value3"},
                {"f.g", 1234}
        };
    }

    @Test(dataProvider = "xmlEscapeDataProvider")
    public void testXmlValueEscaping(String input, String expectedOutput) {

        Assert.assertEquals(XmlValueEscaper.escape(input), expectedOutput);
    }

    @Test
    public void testXmlWellFormedCheck() {

        SAXParserFactory parserFactory = XmlValueEscaper.PARSER_FACTORY;
        Assert.assertNotNull(parserFactory);
        Assert.assertTrue(XmlValueEscaper.isWellFormed(parserFactory, "<a><b x=\"1&amp;2\">p&lt;w</b></a>"));
        Assert.assertTrue(XmlValueEscaper.isWellFormed(parserFactory, "<a><![CDATA[p&<w]]></a>"));
        Assert.assertTrue(XmlValueEscaper.isWellFormed(parserFactory, "<a>" + XmlValueEscaper.escape("&#0;") + "</a>"));
        Assert.assertFalse(XmlValueEscaper.isWellFormed(parserFactory, "<a>pass&word</a>"));
        Assert.assertFalse(XmlValueEscaper.isWellFormed(parserFactory, "<a b=\"x\"y\"/>"));
        Assert.assertFalse(XmlValueEscaper.isWellFormed(parserFactory, "<a><b></a>"));
        Assert.assertFalse(XmlValueEscaper.isWellFormed(parserFactory, "<a>&#0;</a>"));
    }

    @Test
    public void testParserFactoryNotCreatedWhenFeatureUnsupported() {

        Assert.assertNull(XmlValueEscaper.createParserFactory(UnsupportedFeatureParserFactory.class.getName()));
    }

    @Test
    public void testParserFactoryNotCreatedWhenParserSetupFails() {

        Assert.assertNull(XmlValueEscaper.createParserFactory(FailingParserFactory.class.getName()));
    }

    @Test
    public void testXmlOutputUnchangedWithoutParserFactory() {

        String template = "<UserManager><Password>{{super_admin.password}}</Password></UserManager>";
        String renderedTemplate = "<UserManager><Password>pass&word</Password></UserManager>";
        Map<String, Object> context = new HashMap<>();
        context.put("super_admin", new HashMap<>(Collections.singletonMap("password", "pass&word")));

        Assert.assertEquals(JinjaParser.getWellFormedXml(new Jinjava(), template, context, renderedTemplate,
                "user-mgt.xml", null), renderedTemplate);
        Assert.assertEquals(JinjaParser.getWellFormedXml(new Jinjava(), template, context, renderedTemplate,
                "user-mgt.xml", XmlValueEscaper.PARSER_FACTORY),
                "<UserManager><Password>pass&amp;word</Password></UserManager>");
    }

    @Test
    public void testEscapeContextKeepsNonStringValues() {

        Map<String, Object> nested = new HashMap<>();
        nested.put("password", "a&b");
        nested.put("port", 9443);
        Map<String, Object> context = new HashMap<>();
        context.put("admin", nested);
        context.put("list", Arrays.asList("x<y", true));

        Map<String, Object> escaped = XmlValueEscaper.escapeContext(context);

        Assert.assertEquals(((Map) escaped.get("admin")).get("password"), "a&amp;b");
        Assert.assertEquals(((Map) escaped.get("admin")).get("port"), 9443);
        Assert.assertEquals(escaped.get("list"), Arrays.asList("x&lt;y", true));
        Assert.assertEquals(nested.get("password"), "a&b", "Original context should not be modified");
    }

    @Test
    public void testXmlTemplatesRenderedWithEscapedValuesOnlyWhenMalformed() throws Exception {

        Path templateDir = Files.createTempDirectory("templates");
        try {
            File xmlTemplate = templateDir.resolve("user-mgt.xml.j2").toFile();
            File propertiesTemplate = templateDir.resolve("test.properties.j2").toFile();
            Files.write(xmlTemplate.toPath(), ("<UserManager><Password>{{super_admin.password}}</Password>"
                    + "<Store url=\"{{database.url}}\"/></UserManager>").getBytes(StandardCharsets.UTF_8));
            Files.write(propertiesTemplate.toPath(), "password={{super_admin.password}}"
                    .getBytes(StandardCharsets.UTF_8));
            Map<String, File> templateFiles = new LinkedHashMap<>();
            templateFiles.put("user-mgt.xml", xmlTemplate);
            templateFiles.put("test.properties", propertiesTemplate);

            Map<String, String> outputs = JinjaParser.parse(getContext("pass&amp;word", "jdbc:h2:a?x=1&amp;y=2"),
                    templateFiles);
            Assert.assertEquals(outputs.get("user-mgt.xml").trim(), "<UserManager><Password>pass&amp;word</Password>"
                    + "<Store url=\"jdbc:h2:a?x=1&amp;y=2\"/></UserManager>");
            Assert.assertEquals(outputs.get("test.properties").trim(), "password=pass&amp;word");

            outputs = JinjaParser.parse(getContext("p&ss<wo\"rd", "jdbc:h2:a?x=1&amp;y=2"), templateFiles);
            Assert.assertEquals(outputs.get("user-mgt.xml").trim(), "<UserManager><Password>p&amp;ss&lt;wo&quot;rd"
                    + "</Password><Store url=\"jdbc:h2:a?x=1&amp;y=2\"/></UserManager>");
            Assert.assertEquals(outputs.get("test.properties").trim(), "password=p&ss<wo\"rd");

            outputs = JinjaParser.parse(getContext("<![CDATA[pass&word]]>", "jdbc:h2:a"), templateFiles);
            Assert.assertEquals(outputs.get("user-mgt.xml").trim(), "<UserManager><Password><![CDATA[pass&word]]>"
                    + "</Password><Store url=\"jdbc:h2:a\"/></UserManager>");
        } finally {
            org.apache.commons.io.FileUtils.deleteDirectory(templateDir.toFile());
        }
    }

    private Context getContext(String password, String databaseUrl) {

        Context context = new Context();
        context.getTemplateData().put("super_admin.password", password);
        context.getTemplateData().put("database.url", databaseUrl);
        return context;
    }

    @DataProvider(name = "xmlEscapeDataProvider")
    public Object[][] xmlEscapeData() {
        return new Object[][]{
                {"SafePass123", "SafePass123"},
                {"pass&word", "pass&amp;word"},
                {"pass&amp;word", "pass&amp;word"},
                {"a&#38;b&#x26;c&lt;d&gt;e&quot;f&apos;g", "a&#38;b&#x26;c&lt;d&gt;e&quot;f&apos;g"},
                {"a&b&amp;c&", "a&amp;b&amp;c&amp;"},
                {"&#xZZ;", "&amp;#xZZ;"},
                {"&#9;&#x10FFFF;&#xE000;", "&#9;&#x10FFFF;&#xE000;"},
                {"&#0;&#x1F;&#xD800;&#xFFFE;&#x110000;", "&amp;#0;&amp;#x1F;&amp;#xD800;&amp;#xFFFE;&amp;#x110000;"},
                {"&#99999999999;", "&amp;#99999999999;"},
                {"a<b>c", "a&lt;b&gt;c"},
                {"a\"b'c", "a&quot;b&apos;c"},
                {"<![CDATA[p&<w]]>", "<![CDATA[p&<w]]>"},
                {"x&<![CDATA[&]]>&", "x&amp;<![CDATA[&]]>&amp;"},
                {"<![CDATA[unterminated&", "&lt;![CDATA[unterminated&amp;"},
                {"", ""},
        };
    }

    /**
     * Parser factory that does not support any feature.
     */
    public static class UnsupportedFeatureParserFactory extends SAXParserFactory {

        @Override
        public SAXParser newSAXParser() throws ParserConfigurationException {

            throw new ParserConfigurationException("Not supported");
        }

        @Override
        public void setFeature(String name, boolean value) throws SAXNotRecognizedException {

            throw new SAXNotRecognizedException(name);
        }

        @Override
        public boolean getFeature(String name) throws SAXNotRecognizedException {

            throw new SAXNotRecognizedException(name);
        }
    }

    /**
     * Parser factory that accepts all features but cannot create a parser.
     */
    public static class FailingParserFactory extends SAXParserFactory {

        @Override
        public SAXParser newSAXParser() throws ParserConfigurationException {

            throw new ParserConfigurationException("Not supported");
        }

        @Override
        public void setFeature(String name, boolean value) {

        }

        @Override
        public boolean getFeature(String name) {

            return false;
        }
    }
}
