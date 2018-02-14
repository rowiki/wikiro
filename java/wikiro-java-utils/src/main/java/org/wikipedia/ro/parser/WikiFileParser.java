package org.wikipedia.ro.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiFile;

public class WikiFileParser extends WikiPartParser<WikiFile> {

    private static final Pattern FILE_START_MATCHER =
        Pattern.compile("\\[\\[([Ff]i[șş]ier|[Ff]ile|[Mm]edia|[Ii]mage|[Ii]magine):([^\\|\\]]+)");
    private static final Pattern PIXELING_MATCHER = Pattern.compile("(\\d+)?x?(\\d+)?(px)?");

    public boolean startsWithMe(String s) {
        Matcher fileStartMatcher = FILE_START_MATCHER.matcher(s);
        if (!fileStartMatcher.find() || 0 < fileStartMatcher.start()) {
            return false;
        }
        return true;
    }

    private static final int STATE_BEFORE_NEW_ELEMENT = 0;
    private static final int STATE_PARSING_FILE_DECLARATION_ELEMENT = 1;
    private Map<String, Pattern> attrPattern = new HashMap<>();

    private Pattern getAttrPattern(String attr) {
        if (attrPattern.containsKey(attr)) {
            return attrPattern.get(attr);
        }
        attrPattern.put(attr, Pattern.compile(attr + "(=([^\\|\\]]+))?"));
        return attrPattern.get(attr);
    }

    @Override
    public ParseResult<WikiFile> parse(String wikiText) {
        if (!startsWithMe(wikiText)) {
            return null;
        }
        StringBuilder initialText = new StringBuilder();
        WikiFile parsedFile = new WikiFile();
        int idx = 0;

        Matcher fileStartMatcher = FILE_START_MATCHER.matcher(wikiText);
        if (fileStartMatcher.find() && 0 == fileStartMatcher.start()) {
            parsedFile.setNamespace(fileStartMatcher.group(1));
            parsedFile.setName(fileStartMatcher.group(2));
            initialText.append(fileStartMatcher.group(0));

            idx = fileStartMatcher.end();
        } else {
            return null;
        }

        int state = STATE_BEFORE_NEW_ELEMENT;
        boolean closed = false;
        Stack<String> bracketStack = new Stack<>();
        StringBuilder partTextBuilder = new StringBuilder();

        while (idx < wikiText.length() && !closed) {
            char crtChar = wikiText.charAt(idx);
            char nextChar = idx + 1 < wikiText.length() ? wikiText.charAt(idx + 1) : 0;
            String thisAndNextChar = String.format("%c%c", crtChar, nextChar);
            int nextIncrement = 1;

            switch (state) {
            case STATE_BEFORE_NEW_ELEMENT:
                if (crtChar == '|') {
                    state = STATE_PARSING_FILE_DECLARATION_ELEMENT;
                }
                break;
            case STATE_PARSING_FILE_DECLARATION_ELEMENT:
                if (("]]".equals(thisAndNextChar) || '|' == crtChar) && bracketStack.isEmpty()) {
                    String partText = partTextBuilder.toString().trim();
                    if (getAttrPattern("thumb").matcher(partText).matches()
                        || getAttrPattern("thumbnail").matcher(partText).matches()
                        || getAttrPattern("miniatura").matcher(partText).matches() || "frame".equals(partText)
                        || "frameless".equals(partText)) {
                        parsedFile.setDisplayType(partText);
                    } else if ("border".equals(partText)) {
                        parsedFile.setBorder(partText);
                    } else if (Arrays.asList("left", "right", "center", "none", "stanga", "dreapta").contains(partText)) {
                        parsedFile.setLocation(partText);
                    } else if (Arrays
                        .asList("baseline", "middle", "sub", "super", "text-top", "text-bottom", "top", "bottom")
                        .contains(partText)) {
                        parsedFile.setAlignment(partText);
                    } else if (getAttrPattern("upright").matcher(partText).matches()
                        || 1 < partText.length() && PIXELING_MATCHER.matcher(partText).matches()) {
                        parsedFile.setSize(partText);
                    } else {
                        Map<String, Consumer<String>> map = new LinkedHashMap<>();
                        map.put("alt", parsedFile::setAlt);
                        map.put("link", parsedFile::setLink);
                        map.put("lang", parsedFile::setLang);
                        boolean paramFound = false;
                        for (Map.Entry<String, Consumer<String>> entry : map.entrySet()) {
                            Pattern pat = getAttrPattern(entry.getKey());
                            Matcher mat = pat.matcher(partText);
                            if (mat.matches()) {
                                entry.getValue().accept(mat.group(2));
                                paramFound = true;
                            }
                        }
                        if (!paramFound) {
                            parsedFile.addCaption(new AggregatingParser().parse(partText).stream()
                                .map(item -> item.getIdentifiedPart()).collect(Collectors.toList()));
                        }
                    }
                    
                    partTextBuilder.setLength(0);

                    if ("]]".equals(thisAndNextChar)) {
                        nextIncrement = 2;
                        closed = true;
                    }
                } else if (Arrays.asList("{{", "[[").contains(thisAndNextChar)) {
                    nextIncrement = 2;
                    bracketStack.push(thisAndNextChar);
                    partTextBuilder.append(thisAndNextChar);
                } else if (!bracketStack.isEmpty() && ("}}".equals(thisAndNextChar) && "{{".equals(bracketStack.peek())
                    || "]]".equalsIgnoreCase(thisAndNextChar) && "[[".equals(bracketStack.peek()))) {
                    nextIncrement = 2;
                    bracketStack.pop();
                    partTextBuilder.append(thisAndNextChar);
                } else {
                    partTextBuilder.append(crtChar);
                }
            }

            initialText.append(wikiText.substring(idx, idx + nextIncrement));
            idx += nextIncrement;
        }
        ParseResult<WikiFile> ret = new ParseResult<>();
        ret.setIdentifiedPart(parsedFile);
        ret.setParsedString(initialText.toString());
        ret.setUnparsedString(wikiText.substring(initialText.length()));
        parsedFile.setInitialText(ret.getParsedString());
        return ret;
    }

}
