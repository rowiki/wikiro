package org.wikipedia.ro.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTemplateParser {

    @Test
    public void testOnlyTemplateNoParams() {
        String text = "{{TemplateNoArgs}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals(parseResult.getParsedString(), text);
        Assert.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals(0, parsedPart.getParams().size());
        Assert.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());

    }

    @Test
    public void testOnlyTemplateNamedParams() {
        String text = "{{TemplateNamedArg|param = param1}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals(parseResult.getParsedString(), text);
        Assert.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals("TemplateNamedArg", parsedPart.getTemplateTitle());
        Assert.assertEquals(1, parsedPart.getParams().size());
        Assert.assertTrue(parsedPart.getParamNames().contains("param"));
        Assert.assertEquals("param1", parsedPart.getParams().get("param"));
    }

    @Test
    public void testTemplateNoParamsWithExtraText() {
        String text = "{{TemplateNoArgs}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals("{{TemplateNoArgs}}", parseResult.getParsedString());
        Assert.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals(0, parsedPart.getParams().size());
        Assert.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());
    }

    @Test
    public void testTemplateNamedAndUnnamedParamsWithExtraText() {
        String text = "{{TemplateManyArgs|p1=v1|vanon}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals("{{TemplateManyArgs|p1=v1|vanon}}", parseResult.getParsedString());
        Assert.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals("TemplateManyArgs", parsedPart.getTemplateTitle());
        Assert.assertEquals(2, parsedPart.getParams().size());
        Assert.assertTrue(parsedPart.getParamNames().contains("p1"));
        Assert.assertEquals("v1", parsedPart.getParams().get("p1"));
        Assert.assertEquals("vanon", parsedPart.getParams().get("1"));
        Assert.assertNull(parsedPart.getParams().get("2"));
        Assert.assertNull(parsedPart.getParams().get("p2"));

    }

    @Test
    public void testInlineTemplate() {
        String text = "{{Template|NonameParam|Param=Arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assert.assertEquals(text, parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        Assert.assertTrue(parseResult.getIdentifiedPart().isSingleLine());
    }

    @Test
    public void testBlockTemplate() {
        String text = "{{Template\n|NonameParam\n|Param = Arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assert.assertEquals(text, parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        Assert.assertFalse(parseResult.getIdentifiedPart().isSingleLine());

        Assert.assertEquals("NonameParam",
            parseResult.getIdentifiedPart().getParam("1").stream().map(Object::toString).collect(Collectors.joining()));
    }

    @Test
    public void testBlockTemplateWithEmptyUnnamedParams() {
        String text = "{{Template|nonameparam||param = arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assert.assertEquals(text, parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        Assert.assertEquals("nonameparam",
            parseResult.getIdentifiedPart().getParam("1").stream().map(Object::toString).collect(Collectors.joining()));
        Assert.assertNull(parseResult.getIdentifiedPart().getParam("2"));
        Assert.assertEquals("arg",
            parseResult.getIdentifiedPart().getParam("param").stream().map(Object::toString).collect(Collectors.joining()));

    }

    @Test
    public void testTemplateWithLinkInParam() {
        String text = "{{Template|param=value [[link]] value}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        WikiTemplate templ = parseResult.getIdentifiedPart();
        Assert.assertEquals(text, parseResult.getParsedString());
        Assert.assertEquals(1, templ.getParamNames().size());
        List<WikiPart> param = templ.getParam("param");
        Assert.assertNotNull(param);
        Assert.assertEquals(3, param.size());
        Assert.assertTrue(param.get(1) instanceof WikiLink);
        Assert.assertEquals("link", ((WikiLink) param.get(1)).getTarget());

    }

    @Test
    public void testTemplateFromRealArticleApcarBaltazar() {
        String text =
            "{{citat|... o artă de caracter, care are pretenția să rămâie ca un stil propriu al ei, nu trebuie să se mărginească la recopierea unor elemente de artă, fără a căuta să imprime un carcater deosebit acestor elemente... Stilului românesc care așteaptă, trebuie să aibă la bază unele elemente naționale, cum și unele produse ale unei arte, ce s-a convenit a se numi arta trecutului... Pentru această conlucrare însă a elementului primitiv trebuie ceva mai mult decât aimpla lui alipire la produsele timpurilor moderne... prin urmare, pe un fond național să se așeze o compoziție decorativă nouă, care să corespundă principiilor stricte de artă, o artă așa cum o înțeleg artiștii timpurilor noastre, adică o artă cu proporții, cu armonie și mai presus de toate cu originalitate. În ce privește această ultimă calitate, să ne ferim cât mai mult de nefericitele importațiuni străine, ele înseși fiind uneori produse imperfecte sub raportul decorativ.<ref name=petru32/>|Apcar Baltazar: ''[[s:Spre un stil românesc|Spre un stil românesc]]'', în ziarul ''[[Viața Românească]]'' din [[noiembrie]] [[1908]]}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        WikiTemplate templ = parseResult.getIdentifiedPart();
        Assert.assertEquals(text, parseResult.getParsedString());
        Assert.assertEquals(2, templ.getParamNames().size());

        List<WikiPart> param1 = templ.getParam("1");
        Assert.assertNotNull(param1);
        Assert.assertEquals(
            "... o artă de caracter, care are pretenția să rămâie ca un stil propriu al ei, nu trebuie să se mărginească la recopierea unor elemente de artă, fără a căuta să imprime un carcater deosebit acestor elemente... Stilului românesc care așteaptă, trebuie să aibă la bază unele elemente naționale, cum și unele produse ale unei arte, ce s-a convenit a se numi arta trecutului... Pentru această conlucrare însă a elementului primitiv trebuie ceva mai mult decât aimpla lui alipire la produsele timpurilor moderne... prin urmare, pe un fond național să se așeze o compoziție decorativă nouă, care să corespundă principiilor stricte de artă, o artă așa cum o înțeleg artiștii timpurilor noastre, adică o artă cu proporții, cu armonie și mai presus de toate cu originalitate. În ce privește această ultimă calitate, să ne ferim cât mai mult de nefericitele importațiuni străine, ele înseși fiind uneori produse imperfecte sub raportul decorativ.<ref name=petru32/>",
            param1.stream().map(Object::toString).collect(Collectors.joining()));

        List<WikiPart> param2 = templ.getParam("2");
        Assert.assertNotNull(param2);
        Assert.assertEquals(
            "Apcar Baltazar: ''[[s:Spre un stil românesc|Spre un stil românesc]]'', în ziarul ''[[Viața Românească]]'' din [[noiembrie]] [[1908]]",
            param2.stream().map(Object::toString).collect(Collectors.joining()));
    }
}
