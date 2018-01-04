package org.wikipedia.ro.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.Wiki;
import org.xml.sax.SAXException;

public class PageUtils {
    private static Pattern templateStartPattern = Pattern.compile("\\{\\{");

    public static List<WikiTemplate> getTemplatesInText(String text) {
        List<WikiTemplate> ret = new ArrayList<WikiTemplate>();

        Matcher templateStartMatcher = templateStartPattern.matcher(text);
        while (templateStartMatcher.find()) {
            WikiTemplate template = new WikiTemplate(text.substring(templateStartMatcher.start()));
            ret.add(template);
        }
        return ret;
    }

    private static int getLength(Node node) {
        int textLength = 0;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node eachChild = childNodes.item(i);
            if (Node.TEXT_NODE == eachChild.getNodeType()) {
                textLength += eachChild.getTextContent().length();
            } else {
                textLength += getLength(eachChild);
            }
        }
        return textLength;
    }

    private static int getRefMarkLength(Node node) {
        int textLength = 0;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node eachChild = childNodes.item(i);
            if (Node.ELEMENT_NODE == eachChild.getNodeType()) {
                NamedNodeMap childAttrs = eachChild.getAttributes();
                if (null != childAttrs) {
                    Node classNode = childAttrs.getNamedItem("class");
                    if (null != classNode && classNode.getNodeValue().contains("reference")) {
                        textLength += eachChild.getTextContent().length();
                    }
                }
            }
        }
        return textLength;
    }

    public static int getProseSize(Wiki wiki, String articleTitle) {
        String htmlArticle;
        Document doc = null;
        try {
            htmlArticle = wiki.getRenderedText(articleTitle);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new ByteArrayInputStream(htmlArticle.getBytes("UTF-8")));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        int readableSize = -1; 
        if (null != doc) {
            Node bodyNode = doc.getDocumentElement();

            NodeList paraList = doc.getElementsByTagName("p");
            int proseSize = 0;
            int refmarkSize = 0;
            for (int i = 0; i < paraList.getLength(); i++) {
                Node paraNode = paraList.item(i);
                if (paraNode.getParentNode().equals(bodyNode)) {
                    proseSize += getLength(paraNode);
                    refmarkSize += getRefMarkLength(paraNode);
                }
            }

            // System.out.println(String.format("Article %s total size %d refs %d resulting size %d", eachArticle,
            // proseSize, refmarkSize, proseSize - refmarkSize));

            readableSize = proseSize - refmarkSize;
        }

        return readableSize;
    }
}
