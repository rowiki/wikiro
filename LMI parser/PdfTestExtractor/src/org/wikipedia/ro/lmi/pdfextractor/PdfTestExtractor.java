/*
 * To change this template, choose Tools counties.put("Templates
 * and open the template in the editor.
 */
package org.wikipedia.ro.lmi.pdfextractor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.DocumentException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 *
 * @author acipu
 */
public class PdfTestExtractor {

    private static class CountyData {
        public String name;
        public int startPage, endPage;
        public CountyData(String name, int start, int end) {
            this.name = name;
            this.startPage = start;
            this.endPage = end;
        }
    }

     /** The original PDF that will be parsed. */
    public static final String pdf = "/home/andrei/pywikibot-core/LMI2015.pdf";
    /** The resulting text file. */
    public static String txt = "/home/andrei/pywikibot-core/LMI2015_**.txt";

    public static final Hashtable<String, CountyData> counties = new Hashtable<String, CountyData>();

    private static String setCountyCode(String code) {
        txt = txt.replaceFirst("_.{2}", "_" + code);
        return txt;
    }

    private static String getCountyFullName(String code) {
        return counties.get(code).name;
    }

    private static int getCountyStartPage(String code) {
        return counties.get(code).startPage;
    }

    private static int getCountyEndPage(String code) {
        return counties.get(code).endPage;
    }

    private static void buildDatabase() {
        counties.put("B.", new CountyData("București", 501, 733));
        counties.put("AB", new CountyData("Alba", 3, 64));
        counties.put("AR", new CountyData("Arad", 65, 102));
        counties.put("AG", new CountyData("Argeș", 103, 195));
        counties.put("BC", new CountyData("Bacău", 196, 227));
        counties.put("BH", new CountyData("Bihor", 228, 272));
        counties.put("BN", new CountyData("Bistrița-Năsăud", 273, 347));
        counties.put("BT", new CountyData("Botoșani", 348, 393));
        counties.put("BV", new CountyData("Brașov", 394, 482));
        counties.put("BR", new CountyData("Brăila", 483, 500));
        counties.put("BZ", new CountyData("Buzău", 734, 821));
        counties.put("CS", new CountyData("Caraș-Severin", 822, 900));
        counties.put("CL", new CountyData("Călărași", 901, 927));
        counties.put("CJ", new CountyData("Cluj", 928, 1081));
        counties.put("CT", new CountyData("Constanța", 1082, 1147));
        counties.put("CV", new CountyData("Covasna", 1148, 1198));
        counties.put("DB", new CountyData("Dâmbovița", 1199, 1312));
        counties.put("DJ", new CountyData("Dolj", 1313, 1372));
        counties.put("GL", new CountyData("Galați", 1376, 1398));
        counties.put("GR", new CountyData("Giurgiu", 1399, 1444));
        counties.put("GJ", new CountyData("Gorj", 1445, 1486));
        counties.put("HR", new CountyData("Harghita", 1487, 1549));
        counties.put("HD", new CountyData("Hunedoara", 1550, 1595));
        counties.put("IL", new CountyData("Ialomița", 1596, 1614));
        counties.put("IS", new CountyData("Iași", 1615, 1758));
        counties.put("IF", new CountyData("Ilfov", 1759, 1831));
        counties.put("MM", new CountyData("Maramureș", 1832, 1885));
        counties.put("MH", new CountyData("Mehedinți", 1886, 1934));
        counties.put("MS", new CountyData("Mureș", 1935, 2026));
        counties.put("NT", new CountyData("Neamț", 2027, 2072));
        counties.put("OT", new CountyData("Olt", 2073, 2133));
        counties.put("PH", new CountyData("Prahova", 2134, 2224));
        counties.put("SM", new CountyData("Satu Mare", 2225, 2250));
        counties.put("SJ", new CountyData("Sălaj", 2251, 2300));
        counties.put("SB", new CountyData("Sibiu", 2301, 2397));
        counties.put("SV", new CountyData("Suceava", 2398, 2442));
        counties.put("TR", new CountyData("Teleorman", 2443, 2476));
        counties.put("TM", new CountyData("Timiș", 2477, 2507));
        counties.put("TL", new CountyData("Tulcea", 2508, 2563));
        counties.put("VS", new CountyData("Vaslui", 2564, 2604));
        counties.put("VL", new CountyData("Vâlcea", 2605, 2673));
        counties.put("VN", new CountyData("Vrancea", 2674, 2709));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
        throws DocumentException, IOException{
        buildDatabase();
        //for (int i = 1; i <= reader.getNumberOfPages(); i++) {
        //for (int i = 171; i <= 385; i++) {
        for (String county: counties.keySet()) {
            //String county = "AR";
            setCountyCode(county);
            PdfReader reader = new PdfReader(pdf);
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            PrintStream out = new PrintStream(new FileOutputStream(txt), true, "UTF8");
            MyTextExtractionStrategy strategy = new MyTextExtractionStrategy(county, getCountyFullName(county));
            for (int i = getCountyStartPage(county); i <= getCountyEndPage(county); i++) {
            //for (int i = 77; i <= 78; i++) {
                try{
                    parser.processContent(i, strategy);
                    strategy.nextPage();
                }
                catch(Exception e){
                    out.println("Page " + i + " had an error: " + e.toString());
                }
            }
            out.print(strategy.getResultantText());
            out.flush();
            out.close();
        }
    }
}
