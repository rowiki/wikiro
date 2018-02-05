package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.PlainText;

public class WikiTextParser extends WikiPartParser<PlainText> {

    @Override
    public boolean startsWithMe(String wikiText) {
        return true;
    }

    @Override
    public ParseResult<PlainText> parse(String wikiText) {
        StringBuilder textBuilder = new StringBuilder();

        int idx = 0;
        while (idx < wikiText.length()) {
            boolean othersTakeItFromHere = false;
            for (WikiPartParser<?> eachParser : AggregatingParser.ALL_PARSERS) {
                if (!(eachParser instanceof WikiTextParser)) {
                    othersTakeItFromHere = othersTakeItFromHere || eachParser.startsWithMe(wikiText.substring(idx));
                }
            }
            if (othersTakeItFromHere) {
                PlainText foundPart = new PlainText(textBuilder.toString());
                foundPart.setInitialText(textBuilder.toString());
                return new ParseResult<PlainText>(foundPart, textBuilder.toString(), wikiText.substring(idx));
            }
            textBuilder.append(wikiText.charAt(idx));
            idx++;
        }
        PlainText foundPart = new PlainText(textBuilder.toString());
        foundPart.setInitialText(textBuilder.toString());
        return new ParseResult<PlainText>(foundPart, textBuilder.toString(), "");
    }

}
