package org.wikipedia.ro.villagesfixer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;

public class TestSocecReplacement {

    @DisplayName("Cătina")
    @Test
    public void testReplaceOldKingdomRef() {
        InputStream catinaIS = this.getClass().getResourceAsStream("/16600937.txt");
        String catina = new BufferedReader(new InputStreamReader(catinaIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        
        String out = FixVillages.rereferenceSocec(catina);
        
        Pattern refPattern = Pattern.compile("(<ref[^>]*>)(.*?)</ref>");
        Matcher refMatcher = refPattern.matcher(out);
        int socecCount = 0;
        while (refMatcher.find()) {
            String ref = refMatcher.group(2);
            if (!ref.trim().startsWith("{{")) {
                continue;
            }
            WikiTemplateParser templateParser = new WikiTemplateParser();
            ParseResult<WikiTemplate> parseResult = templateParser.parse(ref);
            WikiTemplate template = parseResult.getIdentifiedPart();
            
            if ("Citat Anuarul Socec 1925".equals(template.getTemplateTitle())) {
                socecCount++;
                Assert.assertEquals("4", template.getParams().get("volum"));
                Assert.assertEquals("Comuna Cătina", template.getParams().get("titlu"));
                Assert.assertEquals("121", template.getParams().get("pagină"));
                Assert.assertEquals("129", template.getParams().get("pagină-link"));
            }
        }
        Assert.assertTrue("Socec references should have been found, but none found", 0 < socecCount);
       
    }
    
    @DisplayName("Bicazu Ardelean")
    @Test
    public void testReplaceTransylvaniaRef() {
        InputStream catinaIS = this.getClass().getResourceAsStream("/16606273.txt");
        String catina = new BufferedReader(new InputStreamReader(catinaIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        
        String out = FixVillages.rereferenceSocec(catina);
        
        Pattern refPattern = Pattern.compile("(<ref[^>]*>)(.*?)</ref>");
        Matcher refMatcher = refPattern.matcher(out);
        int socecCount = 0;
        while (refMatcher.find()) {
            String ref = refMatcher.group(2);
            if (!ref.trim().startsWith("{{")) {
                continue;
            }
            WikiTemplateParser templateParser = new WikiTemplateParser();
            ParseResult<WikiTemplate> parseResult = templateParser.parse(ref);
            WikiTemplate template = parseResult.getIdentifiedPart();
            
            if ("Citat Anuarul Socec 1925".equals(template.getTemplateTitle())) {
                socecCount++;
                Assert.assertEquals("5.2", template.getParams().get("volum"));
                Assert.assertEquals("Comuna Bicaz", template.getParams().get("titlu"));
                Assert.assertEquals("311", template.getParams().get("pagină"));
                Assert.assertEquals("1327", template.getParams().get("pagină-link"));
            }
        }
        Assert.assertTrue("Socec references should have been found, but none found", 0 < socecCount);
       
    }
}
