package org.wikipedia.ro.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.table.WikiTable;
import org.wikipedia.ro.model.table.WikiTableCell;
import org.wikipedia.ro.model.table.WikiTableRow;

public class WikiTableParser extends WikiPartParser<WikiTable> {

    public boolean startsWithMe(String wikiText) {
        return null != wikiText && wikiText.startsWith("{|");
    }

    private static final int STATE_INITIAL = -1;
    private static final int STATE_TABLE_STARTED = 0;
    private static final int STATE_READING_ATTRIB = 2;
    private static final int STATE_BEGINNING_CONTAINED_ELEM = 4;

    @Override
    public ParseResult<WikiTable> parse(String wikiText) {
        if (!startsWithMe(wikiText)) {
            return null;
        }

        WikiTable parsedTable = new WikiTable();
        int idx = 0;
        int state = STATE_INITIAL;
        boolean closed = false;
        StringBuilder initialText = new StringBuilder();
        StringBuilder partTextBuilder = new StringBuilder();

        while (idx < wikiText.length() && !closed) {
            char crtChar = wikiText.charAt(idx);
            char nextChar = idx + 1 < wikiText.length() ? wikiText.charAt(idx + 1) : 0;
            char prevChar = 0 < idx ? wikiText.charAt(idx - 1) : 0;
            int nextIncrement = 1;

            switch (state) {
            case STATE_INITIAL: // before {|
                if (crtChar == '{' && nextChar == '|') {
                    nextIncrement = 2;
                    state = STATE_TABLE_STARTED;
                }
                break;
            case STATE_TABLE_STARTED: // after {|
                if (crtChar == '\n') {
                    state = STATE_BEGINNING_CONTAINED_ELEM; // beginning of contained element
                } else if (!Character.isWhitespace(crtChar)) {
                    state = STATE_READING_ATTRIB; // reading table attrib
                    nextIncrement = 0;
                }
                break;
            case STATE_READING_ATTRIB:
                if (crtChar == '\n') {
                    List<ParseResult<WikiPart>> parsedAttribParts =
                        new AggregatingParser().parse(partTextBuilder.toString());
                    parsedTable.setAttribs(parsedAttribParts.stream().map(parseRes -> parseRes.getIdentifiedPart())
                        .collect(Collectors.toList()));
                    partTextBuilder.setLength(0);
                    state = STATE_BEGINNING_CONTAINED_ELEM;
                } else {
                    partTextBuilder.append(crtChar);
                }
                break;
            case STATE_BEGINNING_CONTAINED_ELEM:
                if (crtChar == '|' && nextChar == '+') {
                    ParseResult<WikiTableCell> enclosedCaption = parseTableCell(wikiText.substring(idx - 1));
                    parsedTable.addSubPart(enclosedCaption.getIdentifiedPart());
                    nextIncrement = enclosedCaption.getParsedString().length() - 1;
                } else if (crtChar == '{' && nextChar == '|') {
                    ParseResult<WikiTable> enclosedTable = new WikiTableParser().parse(wikiText.substring(idx));
                    parsedTable.addSubPart(enclosedTable.getIdentifiedPart());
                    nextIncrement = enclosedTable.getParsedString().length();
                } else if (crtChar == '|' && nextChar == '-') {
                    ParseResult<WikiTableRow> enclosedRow = parseTableRow(wikiText.substring(idx));
                    parsedTable.addSubPart(enclosedRow.getIdentifiedPart());
                    nextIncrement = enclosedRow.getParsedString().length();
                } else if (crtChar == '!' && nextChar != '!' && nextChar != '+' && nextChar != '-' && prevChar == '\n') {
                    WikiTableRow headerRow = null;
                    if (!parsedTable.getSubParts().isEmpty()) {
                        WikiPart candidateRow = parsedTable.getSubParts().get(parsedTable.getSubParts().size() - 1);
                        if (candidateRow instanceof WikiTableRow) {
                            headerRow = (WikiTableRow) candidateRow;
                        }
                    }
                    if (null == headerRow) {
                        headerRow = new WikiTableRow();
                        parsedTable.addSubPart(headerRow);
                    }
                    ParseResult<WikiTableCell> enclosedHeaderCell = parseTableCell(wikiText.substring(idx - 1));
                    headerRow.addSubPart(enclosedHeaderCell.getIdentifiedPart());
                    nextIncrement = enclosedHeaderCell.getParsedString().length();
                    
                } else if (crtChar == '|' && nextChar == '}') {
                    closed = true;
                    nextIncrement = 2;
                    state = STATE_INITIAL;
                }
                break;
            default:
                break;
            }
            initialText.append(wikiText.substring(idx, idx + nextIncrement));
            idx += nextIncrement;
        }
        ParseResult<WikiTable> ret = new ParseResult<>();
        ret.setIdentifiedPart(parsedTable);
        ret.setParsedString(initialText.toString());
        ret.setUnparsedString(wikiText.substring(initialText.length()));
        parsedTable.setInitialText(ret.getParsedString());
        return ret;
    }

    private ParseResult<WikiTableRow> parseTableRow(String wikiText) {
        if (null == wikiText || !wikiText.startsWith("|-")) {
            return null;
        }

        int state = STATE_INITIAL;
        StringBuilder initialTextBuilder = new StringBuilder();
        StringBuilder workingElemBuilder = new StringBuilder();
        boolean closed = false;
        int idx = 0;
        WikiTableRow parsedRow = new WikiTableRow();

        while (idx < wikiText.length() && !closed) {
            char crtChar = wikiText.charAt(idx);
            char nextChar = 1 + idx < wikiText.length() ? wikiText.charAt(1 + idx) : 0;
            char prevChar = 0 == idx ? '\0' : wikiText.charAt(idx - 1);
            int nextIncrement = 1;

            switch (state) {
            case STATE_INITIAL:
                if (crtChar == '|' && nextChar == '-') {
                    nextIncrement = 2;
                    state = STATE_READING_ATTRIB;
                }
                break;
            case STATE_READING_ATTRIB:
                if (crtChar == '\n') {
                    state = STATE_BEGINNING_CONTAINED_ELEM;
                    parsedRow.setAttribs(new AggregatingParser().parse(workingElemBuilder.toString()).stream()
                        .map(eachElem -> eachElem.getIdentifiedPart()).collect(Collectors.toList()));
                    workingElemBuilder.setLength(0);
                } else {
                    workingElemBuilder.append(crtChar);
                }
                break;
            case STATE_BEGINNING_CONTAINED_ELEM:
                if (prevChar == '\n' && (crtChar == '!' || crtChar == '|' && nextChar != '}' && nextChar != '-')) {
                    ParseResult<WikiTableCell> parsedCell = parseTableCell(wikiText.substring(idx - 1));
                    parsedRow.addSubPart(parsedCell.getIdentifiedPart());
                    nextIncrement = parsedCell.getParsedString().length() - 1;
                }
                if ((crtChar == '!' || crtChar == '|') && crtChar == nextChar) {
                    ParseResult<WikiTableCell> parsedCell = parseTableCell(wikiText.substring(idx));
                    parsedRow.addSubPart(parsedCell.getIdentifiedPart());
                    nextIncrement = parsedCell.getParsedString().length();
                }
                if (crtChar == '|' && (nextChar == '-' || nextChar == '}')) {
                    closed = true;
                    nextIncrement = 0;
                }
                break;
            default:
                break;
            }

            initialTextBuilder.append(wikiText.substring(idx, idx + nextIncrement));
            idx += nextIncrement;
        }
        ParseResult<WikiTableRow> res = new ParseResult<>();
        res.setIdentifiedPart(parsedRow);
        res.setParsedString(initialTextBuilder.toString());
        res.setUnparsedString(wikiText.substring(initialTextBuilder.length()));
        parsedRow.setInitialText(res.getParsedString());
        return res;
    }

    private ParseResult<WikiTableCell> parseTableCell(String wikiText) {
        if (null == wikiText || (!wikiText.startsWith("\n|") && !wikiText.startsWith("\n!") && !wikiText.startsWith("||")
            && !wikiText.startsWith("!!") && !wikiText.startsWith("\n|+"))) {
            return null;
        }

        int state = STATE_INITIAL;
        StringBuilder initialTextBuilder = new StringBuilder();
        Stack<String> bracketStack = new Stack<>();
        StringBuilder workingElemBuilder = new StringBuilder();
        boolean closed = false;
        int idx = 0;
        WikiTableCell parsedCell = new WikiTableCell();

        while (idx < wikiText.length() && !closed) {
            char crtChar = wikiText.charAt(idx);
            char nextChar = 1 + idx < wikiText.length() ? wikiText.charAt(1 + idx) : 0;
            String thisAndNextChar = String.format("%c%c", crtChar, nextChar);
            int nextIncrement = 1;

            switch (state) {
            case STATE_INITIAL:
                String cellSeparator = thisAndNextChar;
                nextIncrement = 2;
                if ("\n|".equals(thisAndNextChar) && 2 + idx < wikiText.length() && '+' == wikiText.charAt(2 + idx)) {
                    nextIncrement++;
                    cellSeparator = cellSeparator + '+';
                }
                parsedCell.setCellSeparator(cellSeparator);
                state = STATE_READING_ATTRIB;
                break;
            case STATE_READING_ATTRIB:
                if (Arrays.asList("||", "!!", "\n|", "\n!", "|}", "|-").contains(thisAndNextChar)) {
                    parsedCell.setSubParts(new AggregatingParser().parse(workingElemBuilder.toString()).stream()
                        .map(elem -> elem.getIdentifiedPart()).collect(Collectors.toList()));
                    workingElemBuilder.setLength(0);
                    nextIncrement = 0;
                    closed = true;
                } else if (crtChar == '|' && nextChar != '|' && bracketStack.isEmpty()) {
                    parsedCell.setAttribs(new AggregatingParser().parse(workingElemBuilder.toString()).stream()
                        .map(elem -> elem.getIdentifiedPart()).collect(Collectors.toList()));
                    workingElemBuilder.setLength(0);
                    state = STATE_BEGINNING_CONTAINED_ELEM;
                } else if ("{{".equals(thisAndNextChar) || "[[".equals(thisAndNextChar)) {
                    bracketStack.push(thisAndNextChar);
                    nextIncrement = 2;
                    workingElemBuilder.append(thisAndNextChar);
                } else if ("}}".equals(thisAndNextChar) && "{{".equals(bracketStack.peek())
                    || "]]".equals(thisAndNextChar) && "[[".equals(bracketStack.peek())) {
                    bracketStack.pop();
                    nextIncrement = 2;
                    workingElemBuilder.append(thisAndNextChar);
                } else {
                    if ('"' == crtChar) {
                        if (!bracketStack.isEmpty() && "\"".equals(bracketStack.peek())) {
                            bracketStack.pop();
                        } else {
                            bracketStack.push("\"");
                        }
                    }
                    workingElemBuilder.append(crtChar);
                }
                break;
            case STATE_BEGINNING_CONTAINED_ELEM:
                if (Arrays.asList("||", "!!", "\n|", "\n!", "|}", "|-").contains(thisAndNextChar)
                    && bracketStack.isEmpty()) {
                    parsedCell.setSubParts(new AggregatingParser().parse(workingElemBuilder.toString()).stream()
                        .map(elem -> elem.getIdentifiedPart()).collect(Collectors.toList()));
                    nextIncrement = 0;
                    closed = true;
                } else if (Arrays.asList("{{", "[[", "{|").contains(thisAndNextChar)) {
                    nextIncrement = 2;
                    bracketStack.push(thisAndNextChar);
                } else if ("}}".equals(thisAndNextChar) && !bracketStack.isEmpty() && "{{".equals(bracketStack.peek())
                    || "]]".equals(thisAndNextChar) && !bracketStack.isEmpty() && "[[".equals(bracketStack.peek())
                    || "|}".equals(thisAndNextChar) && !bracketStack.isEmpty() && "|}".equals(bracketStack.peek())) {
                    nextIncrement = 2;
                    bracketStack.pop();
                } else {
                    workingElemBuilder.append(crtChar);
                }
                break;
            default:
                break;
            }

            initialTextBuilder.append(wikiText.substring(idx, idx + nextIncrement));
            idx += nextIncrement;
        }
        ParseResult<WikiTableCell> res = new ParseResult<>();
        res.setIdentifiedPart(parsedCell);
        res.setParsedString(initialTextBuilder.toString());
        res.setUnparsedString(wikiText.substring(initialTextBuilder.length()));
        parsedCell.setInitialText(res.getParsedString());
        return res;
    }

}
