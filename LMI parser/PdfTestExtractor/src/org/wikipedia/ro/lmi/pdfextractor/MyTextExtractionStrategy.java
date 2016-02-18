/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wikipedia.ro.lmi.pdfextractor;

import com.itextpdf.text.pdf.CMapAwareDocumentFont;
import com.itextpdf.text.pdf.parser.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author acipu
 */
public class MyTextExtractionStrategy implements TextExtractionStrategy {

    /** set to true for debugging */
    static boolean DUMP_STATE = false;

    /** perpendicular distance (in Vector units) of the end of the page */
    static final int pageSize = 520;

    /** a summary of all found text */
    private final List<TextChunk> locationalResult = new ArrayList<TextChunk>();

    private final String[] templateParams = {"\n| Cod = ", "\n| Denumire = ",
        "\n| Localitate = ", "\n| Adresă = ", "\n| Datare = ",
        "\n| Creatori = "};

    /** a list of all found columns; it should differ from county to county */
    private ArrayList<Integer> columnsOffset = new ArrayList<Integer>();

    private final String countyName;
    private final String countyCode;
    private int pageNumber;

    /**
     * Creates a new text extraction renderer.
     */
    public MyTextExtractionStrategy(String code, String county) {
        countyCode = code;
        countyName = county;
        pageNumber = 0;
    }

    public int nextPage()
    {
        return ++pageNumber;
    }

    /**
     * Reset the columns; can happen if they vary significantly between pages
     */
    public void resetColumnOffsets()
    {
        columnsOffset = new ArrayList<Integer>();
    }

    /**
     * Don't keep the found text any more - we have delivered it to the superior layer
     */
    private void resetText()
    {
        locationalResult.clear();
    }

    /**
     * @see com.itextpdf.text.pdf.parser.RenderListener#beginTextBlock()
     */
    public void beginTextBlock(){
    }

    /**
     * @see com.itextpdf.text.pdf.parser.RenderListener#endTextBlock()
     */
    public void endTextBlock(){
    }

    /**
     *
     * @see com.itextpdf.text.pdf.parser.RenderListener#renderText(com.itextpdf.text.pdf.parser.TextRenderInfo)
     */
    public void renderText(TextRenderInfo renderInfo) {
    	LineSegment segment = renderInfo.getBaseline();
        //bold text contains table header, so just throw it out
        if(renderInfo.getFont().getPostscriptFontName().contains("Bold") ||
            renderInfo.getFont().getPostscriptFontName().contains("Italic"))
            return;

        TextChunk location = new TextChunk(renderInfo.getText(),
                                            segment.getStartPoint(),
                                            segment.getEndPoint(),
                                            4/*renderInfo.getSingleSpaceWidth()*/,
                                            pageNumber * pageSize);
        if (location.text.trim().isEmpty())
            return;
        //System.out.println(renderInfo.getText() + " font " + location.orientationMagnitude);
        //non-vertical text, should be horizontal;
        if(location.orientationMagnitude >= Math.floor(1000 * Math.PI / 2))
            locationalResult.add(location);
    }

    /**
     * Returns the result so far.
     * @return  a String with the resulting text.
     */
    public String getResultantText(){

        //Collections.sort(locationalResult);
        orderChunks();

        if (DUMP_STATE) {
            dumpState();
        }

        ArrayList<String[]> pageArray = new ArrayList<String[]>();
        int line = 0;
        StringBuffer sb = new StringBuffer();

        groupChunks(pageArray);

        //for each monument
        for(line = 0; line < pageArray.size(); line++) {
            StringBuffer sbt = new StringBuffer();
            sbt.append("\n{{ElementLMI");
            int i = 0;
            boolean emptyLine = true;
            //for each column
            for(i = 0; i < pageArray.get(line).length; i++) {
                String value = pageArray.get(line)[i];
                if (value == null)
                    value = "";
                else {
                    value = value.replace("null", "");
                    value = value.trim();
                }
                if(!value.isEmpty())
                    emptyLine = false;
                if(templateParams[i].contains("Localitate")) {
                    value = insertCityLinks(value);
                }
                sbt.append(templateParams[i]);
                sbt.append(value);
            }

            if (emptyLine)
                continue; //move to the next monument

            //append the remaining (hopefuly empty) parameters of the template
            for(; i < templateParams.length; i++) {
                sbt.append(templateParams[i]);
            }
            sbt.append("\n}}");
            sb.append(sbt);
        }

        String result = sb.toString();
        result = result.replace("MONITORUL OFICIAL AL ROMÂNIEI, PARTEA I, Nr. 113 bis/15.II.2016", "");
        result = result.replace("MINISTERUL CULTURII", "");
        result = result.replace("INSTITUTUL NAŢIONAL AL PATRIMONIULUI", "");
        result = result.replace("Destinat exclusiv informarii persoanelor fizice", "");
        result = result.replace("ANEX", "");
        result = result.replace("", "\"");
        result = result.replace("","\"");
        result = result.replace("","\"");
        result = result.replace("","-");

        resetText();

        return result;

    }

    /** Used for debugging only */
    private void dumpState(){
        for (Iterator<TextChunk> iterator = locationalResult.iterator(); iterator.hasNext(); ) {
            TextChunk location = (TextChunk) iterator.next();
            System.out.println(columnsOffset);
            location.printDiagnostics();
            System.out.println();
        }

    }

    private boolean chunksInDifferentColumns(TextChunk thisChunk, TextChunk nextChunk) {
        if(Math.round(nextChunk.distanceFromEndOf(thisChunk)) > 5 &&
           (thisChunk.column >= (columnsOffset.size() - 1) ||
           locateOffsetValue(Math.round(nextChunk.distParallelStart)) > -1))
            return true;
        return false;
    }

    private void orderChunks() {
        Collections.sort(locationalResult);
        if(true){
            locationalResult.get(0).column = locateOffsetValue(Math.round(locationalResult.get(0).distParallelStart), false);
            if(locationalResult.get(0).column < columnsOffset.size())
                columnsOffset.set(locationalResult.get(0).column,
                        Math.round(locationalResult.get(0).distParallelStart));
            else
                columnsOffset.add(Math.round(locationalResult.get(0).distParallelStart));

            for(int i = 0; i < locationalResult.size() - 1; i++)
            {
                TextChunk thisChunk = locationalResult.get(i);
                TextChunk nextChunk = locationalResult.get(i+1);
                //System.out.println(columnsOffset);
                //thisChunk.printDiagnostics();
                //nextChunk.printDiagnostics();

                if (thisChunk.sameLine(nextChunk) &&
                        !chunksInDifferentColumns(thisChunk, nextChunk)) {
                    nextChunk.column = thisChunk.column;
                    //System.out.println(thisChunk.column >= (columnsOffset.size() - 1));
                    //System.out.println(locateOffsetValue(Math.round(nextChunk.distParallelStart)));
                    //System.out.println("Next chunk (" + nextChunk.text + ") has the same column ("+nextChunk.column+")");
                }
                else if (thisChunk.sameLine(nextChunk)) {//new column
                    int index = locateOffsetValue(Math.round(nextChunk.distParallelStart));
                    if(index > -1)
                        nextChunk.column = index;
                    else {
                        System.out.println("Unknown column for offset " +
                                nextChunk.distParallelStart + " chunk " + nextChunk.text);
                        nextChunk.column = thisChunk.column + 1;
                    }
                    if(nextChunk.column < columnsOffset.size())
                        columnsOffset.set(nextChunk.column,
                                Math.round(nextChunk.distParallelStart));
                    else
                        columnsOffset.add(Math.round(nextChunk.distParallelStart));
                }
                else { //new line
                    int index = locateOffsetValue(Math.round(nextChunk.distParallelStart), false);
                    if(index > -1)
                        nextChunk.column = index;
                    else {
                        nextChunk.column = 0;
                    }
                    System.out.println("--New line-- Column " + nextChunk.column + " for offset " +
                                nextChunk.distParallelStart + " chunk " + nextChunk.text);
                }
            }
        }
    }

    private String needSpace(TextChunk lastChunk, TextChunk nextChunk) {
        char last = lastChunk.text.charAt(lastChunk.text.length() - 1);
        char first = nextChunk.text.charAt(0);

        // we only insert a blank space if the trailing character of the previous
        // string wasn't a space, and the leading character of the current string
        // isn't a space
        if (Character.isWhitespace(last) || Character.isWhitespace(first))
            return "";

        float dist = nextChunk.distanceFromEndOf(lastChunk);

        if(!lastChunk.sameLine(nextChunk))
            return " ";

        if (dist < -nextChunk.charSpaceWidth)
            return " ";

        //too much distance, probably a space
        else if (dist > nextChunk.charSpaceWidth/2.0f)
        {
            return " ";
        }

        //if the previous chunk ends in lower case and the new one begins with upper case
        if (Character.isLowerCase(last)
                && Character.isUpperCase(first))
            return " ";

        if(!Character.isLetter(last) &&
                last != '"' &&
                last != '-' &&
                Character.isLetter(first) && lastChunk.column > 0)
            return " ";

        return "";
    }

    private void groupChunks(ArrayList<String[]> pageArray) {
        TextChunk lastChunk = null;
        int line = 0;
        for (TextChunk chunk : locationalResult) {
            if (lastChunk != null){
                if (chunk.sameColumn(lastChunk) && chunk.sameLine(lastChunk)){
                    String temp = pageArray.get(line)[lastChunk.column] +
                                    needSpace(lastChunk, chunk) +
                                    chunk.text;

                    pageArray.get(line)[chunk.column] = temp;
                } else {
                    if(chunk.column == 0 && chunk.text.charAt(0) == countyCode.charAt(0)) {
                        //TODO: 1 letter comparison is not enough
                        pageArray.add(new String[columnsOffset.size()]);
                        line++;
                        pageArray.get(line)[chunk.column] = chunk.text;
                    }
                    else {
                        String temp = pageArray.get(line)[chunk.column] +
                                    needSpace(lastChunk, chunk) +
                                    chunk.text;

                        pageArray.get(line)[chunk.column] = temp;
                    }
                }
            } else {
                pageArray.add(new String[columnsOffset.size()]);
                pageArray.get(line)[chunk.column] = chunk.text;
            }
            lastChunk = chunk;
        }
    }

    private String capitalize(String s){
        StringBuffer sb = new StringBuffer();
        if(s.length() == 0)
            return s;
        //if the string does not start with space, we might want to add one
        if(s.charAt(0) != ' ')
            s = " " + s;
        for(int i = 1; i < s.length(); i++) {
            if(s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '-') {
                if("DE ".equals(s.substring(i, (i+3<s.length())?(i+3):i)) ||
                   "CU ".equals(s.substring(i, (i+3<s.length())?(i+3):i)) ||
                   "DIN ".equals(s.substring(i, (i+4<s.length())?(i+4):i))||
                   "CEL ".equals(s.substring(i, (i+4<s.length())?(i+4):i)))
                    sb.append(s.toLowerCase().charAt(i));
                else
                    sb.append(s.charAt(i));
            }
            else
                sb.append(s.toLowerCase().charAt(i));
        }

        return sb.toString();
    }

    private String insertCityLinks(String value) {
        String[] administrations = {"sat", "localitatea", "localitate componentă", "municipiul", "oraș", "comuna"};

        if(value == null || value == "")
            return value;

        for(String type: administrations) {
            int index = value.indexOf(type);
            int endIndex = -1;
            String temp, ending = null;
            if(index > -1) {
                index += type.length();
                if("sat".equals(type) &&
                        value.substring(index).contains("aparținător")) {//dirty hack?
                    index += " aparţinător".length();
                }
                endIndex = value.indexOf(';', index);

                if(endIndex > -1){
                    ending = value.substring(endIndex);
                    temp = value.substring(index, endIndex);
                }
                else {
                    temp = value.substring(index);
                }
                String capitalized = capitalize(temp);
                String extra = "";
                if(type != "municipiul" && type != "oraș") {
                    extra = ", " + countyName;
                }
                String prefix = (type == "comuna" ? "Comuna " : "") +
                                capitalized + extra;

                String link = (prefix.equals(capitalized)) ?
                            " [[" + prefix + "]]" :
                            " [[" + prefix + "|" + capitalized + "]]";
                value = value.substring(0, index) + link;
                if(endIndex > -1)
                    value = value + ending;
            }
        }
        return value;
    }

    private int locateOffsetValue(int round) {
        return locateOffsetValue(round, true);
    }

    private int locateOffsetValue(int round, boolean precise) {
        int min = Integer.MAX_VALUE;
        int column = 0;
        for(int i = 0; i < columnsOffset.size(); i++)
        {
            int diff = Math.abs(columnsOffset.get(i) - round);
            if (diff <= 1)
                return i;
            if (diff < min) {
                min = diff;
                column = i;
            }
        }

        return precise ? -1 : column;
    }



    /**
     * Represents a chunk of text, it's orientation, and location relative to the orientation vector
     */
    private class TextChunk implements Comparable<TextChunk>{
        /** the text of the chunk */
        final String text;
        /** the starting location of the chunk */
        final Vector startLocation;
        /** the ending location of the chunk */
        final Vector endLocation;
        /** unit vector in the orientation of the chunk */
        final Vector orientationVector;
        /** the orientation as a scalar for quick sorting */
        final int orientationMagnitude;
        /** perpendicular distance to the orientation unit vector (i.e. the Y position in an unrotated coordinate system)
         * we round to the nearest integer to handle the fuzziness of comparing floats */
        final int distPerpendicular;
        /** distance of the start of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
        final float distParallelStart;
        /** distance of the end of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
        final float distParallelEnd;
        /** the width of a single space character in the font of the chunk */
        final float charSpaceWidth;
        /** the column this chunk belongs to */
        public int column;

        public TextChunk(String string, Vector startLocation, Vector endLocation,
                        float charSpaceWidth, int pageOffset) {
            this.text = string.replace('\u0002', ' ').
                                replace('\u0003', '-').
                                replace('\u0004', 'ă').
                                replace('\u0005', 'ț').
                                replace('\u0006', 'ș').
                                replace('\u0007', 'Ț').
                                replace('\u0008', 'Ș').
                                replace('\u0009', 'Ă').
                                replace('\u000b', 'ș').
                                replace('\u000c', 'Ș').
                                replace('\r', 'ț').
                                replace('\u000e', 'Ț');
            this.startLocation = startLocation;
            this.endLocation = endLocation;
            this.charSpaceWidth = charSpaceWidth;
            this.column = -1;

            orientationVector = endLocation.subtract(startLocation).normalize();
            orientationMagnitude = (int)(Math.atan2(orientationVector.get(Vector.I2), orientationVector.get(Vector.I1))*1000);

            //calculate the distance from the origin of the page to our chunk
            // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
            // the two vectors we are crossing are in the same plane, so the result will be purely
            // in the z-axis (out of plane) direction, so we just take the I3 component of the result
            Vector origin = new Vector(0,0,1);
            distPerpendicular = (int)(startLocation.subtract(origin)).cross(orientationVector).get(Vector.I3) + pageOffset;

            distParallelStart = orientationVector.dot(startLocation);
            distParallelEnd = orientationVector.dot(endLocation);
            //printDiagnostics();
        }

        private void printDiagnostics(){
            System.out.println("Text (@" + startLocation + " -> " + endLocation + "): " + text);
            System.out.println("orientationMagnitude: " + orientationMagnitude);
            System.out.println("distPerpendicular: " + distPerpendicular);
            System.out.println("distParallelStart: " + distParallelStart);
            System.out.println("distParallelEnd: " + distParallelEnd);
            System.out.println("column: " + column);
        }

        /**
         * @param as the location to compare to
         * @return true is this location is on the the same line as the other
         */
        public boolean sameLine(TextChunk as){
            if (orientationMagnitude != as.orientationMagnitude) return false;
            if (compareInts(distPerpendicular, as.distPerpendicular) != 0)
                return false;
            return true;
        }

        /**
         * Computes the distance between the end of 'other' and the beginning of this chunk
         * in the direction of this chunk's orientation vector.  Note that it's a bad idea
         * to call this for chunks that aren't on the same line and orientation, but we don't
         * explicitly check for that condition for performance reasons.
         * @param other
         * @return the number of spaces between the end of 'other' and the beginning of this chunk
         */
        public float distanceFromEndOf(TextChunk other){
            float distance = distParallelStart - other.distParallelEnd;
            return distance;
        }

        /**
         * Compares based on orientation, perpendicular distance, then parallel distance
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(TextChunk rhs) {
            if (this == rhs) return 0; // not really needed, but just in case

            int rslt;
            rslt = compareInts(orientationMagnitude, rhs.orientationMagnitude);
            if (rslt != 0) return rslt;

            rslt = compareInts(distPerpendicular, rhs.distPerpendicular);
            if (rslt != 0) return rslt;

            // note: it's never safe to check floating point numbers for equality, and if two chunks
            // are truly right on top of each other, which one comes first or second just doesn't matter
            // so we arbitrarily choose this way.
            rslt = distParallelStart < rhs.distParallelStart ? -1 : 1;

            return rslt;
        }

        /**
         *
         * @param int1
         * @param int2
         * @return comparison of the two integers
         */
        private int compareInts(int int1, int int2){
            if(Math.abs(int1 - int2) <= 1)
                return 0;
            else
                return int1 < int2 ? -1 : 1;
        }

        private boolean sameColumn(TextChunk as) {
            if(column != as.column)
                return false;
            return true;
        }


    }

    /**
     * no-op method - this renderer isn't interested in image events
     * @see com.itextpdf.text.pdf.parser.RenderListener#renderImage(com.itextpdf.text.pdf.parser.ImageRenderInfo)
     * @since 5.0.1
     */
    public void renderImage(ImageRenderInfo renderInfo) {
        // do nothing
    }

}
