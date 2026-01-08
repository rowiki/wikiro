package org.wikipedia.ro.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTemplateParser {

    @Test
    public void testOnlyTemplateNoParams() {
        String text = "{{TemplateNoArgs}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assertions.assertEquals(parseResult.getParsedString(), text);
        Assertions.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(parsedPart);
        Assertions.assertEquals(0, parsedPart.getParams().size());
        Assertions.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());

    }

    @Test
    public void testOnlyTemplateNamedParams() {
        String text = "{{TemplateNamedArg|param = param1}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assertions.assertEquals(parseResult.getParsedString(), text);
        Assertions.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(parsedPart);
        Assertions.assertEquals("TemplateNamedArg", parsedPart.getTemplateTitle());
        Assertions.assertEquals(1, parsedPart.getParams().size());
        Assertions.assertTrue(parsedPart.getParamNames().contains("param"));
        Assertions.assertEquals("param1", parsedPart.getParams().get("param"));
    }

    @Test
    public void testTemplateNoParamsWithExtraText() {
        String text = "{{TemplateNoArgs}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assertions.assertEquals("{{TemplateNoArgs}}", parseResult.getParsedString());
        Assertions.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(parsedPart);
        Assertions.assertEquals(0, parsedPart.getParams().size());
        Assertions.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());
    }

    @Test
    public void testTemplateNamedAndUnnamedParamsWithExtraText() {
        String text = "{{TemplateManyArgs|p1=v1|vanon}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assertions.assertEquals("{{TemplateManyArgs|p1=v1|vanon}}", parseResult.getParsedString());
        Assertions.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(parsedPart);
        Assertions.assertEquals("TemplateManyArgs", parsedPart.getTemplateTitle());
        Assertions.assertEquals(2, parsedPart.getParams().size());
        Assertions.assertTrue(parsedPart.getParamNames().contains("p1"));
        Assertions.assertEquals("v1", parsedPart.getParams().get("p1"));
        Assertions.assertEquals("vanon", parsedPart.getParams().get("1"));
        Assertions.assertNull(parsedPart.getParams().get("2"));
        Assertions.assertNull(parsedPart.getParams().get("p2"));

    }

    @Test
    public void testInlineTemplate() {
        String text = "{{Template|NonameParam|Param=Arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assertions.assertEquals(text, parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        Assertions.assertTrue(parseResult.getIdentifiedPart().isSingleLine());
    }

    @Test
    public void testBlockTemplate() {
        String text = "{{Template\n|NonameParam\n|Param = Arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assertions.assertEquals(text, parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        Assertions.assertFalse(parseResult.getIdentifiedPart().isSingleLine());

        Assertions.assertEquals("NonameParam",
            parseResult.getIdentifiedPart().getParam("1").stream().map(Object::toString).collect(Collectors.joining()));
    }

    @Test
    public void testBlockTemplateWithEmptyUnnamedParams() {
        String text = "{{Template|nonameparam||param = arg}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        Assertions.assertEquals(text, parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        Assertions.assertEquals("nonameparam",
            parseResult.getIdentifiedPart().getParam("1").stream().map(Object::toString).collect(Collectors.joining()));
        Assertions.assertNull(parseResult.getIdentifiedPart().getParam("2"));
        Assertions.assertEquals("arg",
            parseResult.getIdentifiedPart().getParam("param").stream().map(Object::toString).collect(Collectors.joining()));

    }

    @Test
    public void testTemplateWithLinkInParam() {
        String text = "{{Template|param=value [[link]] value}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        WikiTemplate templ = parseResult.getIdentifiedPart();
        Assertions.assertEquals(text, parseResult.getParsedString());
        Assertions.assertEquals(1, templ.getParamNames().size());
        List<WikiPart> param = templ.getParam("param");
        Assertions.assertNotNull(param);
        Assertions.assertEquals(3, param.size());
        Assertions.assertTrue(param.get(1) instanceof WikiLink);
        Assertions.assertEquals("link", ((WikiLink) param.get(1)).getTarget());

    }

    //@Test
    //commented for now: params containing <ref> tags are not yet supported
    public void testTemplateFromRealArticleApcarBaltazar() {
        String text =
            "{{citat|... o artă de caracter, care are pretenția să rămâie ca un stil propriu al ei, nu trebuie să se mărginească la recopierea unor elemente de artă, fără a căuta să imprime un carcater deosebit acestor elemente... Stilului românesc care așteaptă, trebuie să aibă la bază unele elemente naționale, cum și unele produse ale unei arte, ce s-a convenit a se numi arta trecutului... Pentru această conlucrare însă a elementului primitiv trebuie ceva mai mult decât aimpla lui alipire la produsele timpurilor moderne... prin urmare, pe un fond național să se așeze o compoziție decorativă nouă, care să corespundă principiilor stricte de artă, o artă așa cum o înțeleg artiștii timpurilor noastre, adică o artă cu proporții, cu armonie și mai presus de toate cu originalitate. În ce privește această ultimă calitate, să ne ferim cât mai mult de nefericitele importațiuni străine, ele înseși fiind uneori produse imperfecte sub raportul decorativ.<ref name=petru32/>|Apcar Baltazar: ''[[s:Spre un stil românesc|Spre un stil românesc]]'', în ziarul ''[[Viața Românească]]'' din [[noiembrie]] [[1908]]}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);
        WikiTemplate templ = parseResult.getIdentifiedPart();
        Assertions.assertEquals(text, parseResult.getParsedString());
        Assertions.assertEquals(2, templ.getParamNames().size());

        List<WikiPart> param1 = templ.getParam("1");
        Assertions.assertNotNull(param1);
        Assertions.assertEquals(
            "... o artă de caracter, care are pretenția să rămâie ca un stil propriu al ei, nu trebuie să se mărginească la recopierea unor elemente de artă, fără a căuta să imprime un carcater deosebit acestor elemente... Stilului românesc care așteaptă, trebuie să aibă la bază unele elemente naționale, cum și unele produse ale unei arte, ce s-a convenit a se numi arta trecutului... Pentru această conlucrare însă a elementului primitiv trebuie ceva mai mult decât aimpla lui alipire la produsele timpurilor moderne... prin urmare, pe un fond național să se așeze o compoziție decorativă nouă, care să corespundă principiilor stricte de artă, o artă așa cum o înțeleg artiștii timpurilor noastre, adică o artă cu proporții, cu armonie și mai presus de toate cu originalitate. În ce privește această ultimă calitate, să ne ferim cât mai mult de nefericitele importațiuni străine, ele înseși fiind uneori produse imperfecte sub raportul decorativ.<ref name=petru32/>",
            param1.stream().map(Object::toString).collect(Collectors.joining()));

        List<WikiPart> param2 = templ.getParam("2");
        Assertions.assertNotNull(param2);
        Assertions.assertEquals(
            "Apcar Baltazar: ''[[s:Spre un stil românesc|Spre un stil românesc]]'', în ziarul ''[[Viața Românească]]'' din [[noiembrie]] [[1908]]",
            param2.stream().map(Object::toString).collect(Collectors.joining()));
    }
}
