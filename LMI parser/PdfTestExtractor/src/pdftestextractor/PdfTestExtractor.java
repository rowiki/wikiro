/*
 * To change this template, choose Tools counties.put("Templates
 * and open the template in the editor.
 */
package pdftestextractor;

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
        //counties.put("B.", new CountyData("București", 3, 64));
        counties.put("AB", new CountyData("Alba", 3, 64));
        counties.put("AR", new CountyData("Arad", 65, 102));
        //counties.put("AG", new CountyData("Argeș", 103, 64));
        //counties.put("BC", new CountyData("Bacău", 3, 64));
        //counties.put("BH", new CountyData("Bihor", 3, 64));
        //counties.put("BN", new CountyData("Bistrița-Năsăud", 3, 64));
        //counties.put("BT", new CountyData("Botoșani", 3, 64));
        //counties.put("BV", new CountyData("Brașov", 3, 64));
        //counties.put("BR", new CountyData("Brăila", 3, 64));
        //counties.put("BZ", new CountyData("Buzău", 3, 64));
        //counties.put("CS", new CountyData("Caraș-Severin", 3, 64));
        //counties.put("CL", new CountyData("Călărași", 3, 64));
        //counties.put("CJ", new CountyData("Cluj", 3, 64));
        //counties.put("CT", new CountyData("Constanța", 3, 64));
        //counties.put("CV", new CountyData("Covasna", 3, 64));
        //counties.put("DB", new CountyData("Dâmbovița", 3, 64));
        //counties.put("DJ", new CountyData("Dolj", 3, 64));
        //counties.put("GL", new CountyData("Galați", 3, 64));
        //counties.put("GR", new CountyData("Giurgiu", 3, 64));
        //counties.put("GJ", new CountyData("Gorj", 3, 64));
        //counties.put("HR", new CountyData("Harghita", 3, 64));
        //counties.put("HD", new CountyData("Hunedoara", 3, 64));
        //counties.put("IL", new CountyData("Ialomița", 3, 64));
        //counties.put("IS", new CountyData("Iași", 3, 64));
        //counties.put("IF", new CountyData("Ilfov", 3, 64));
        //counties.put("MM", new CountyData("Maramureș", 3, 64));
        //counties.put("MH", new CountyData("Mehedinți", 3, 64));
        //counties.put("MS", new CountyData("Mureș", 3, 64));
        //counties.put("NT", new CountyData("Neamț", 3, 64));
        //counties.put("OT", new CountyData("Olt", 3, 64));
        //counties.put("PH", new CountyData("Prahova", 3, 64));
        //counties.put("SM", new CountyData("Satu Mare", 3, 64));
        //counties.put("SJ", new CountyData("Sălaj", 3, 64));
        //counties.put("SB", new CountyData("Sibiu", 3, 64));
        //counties.put("SV", new CountyData("Suceava", 3, 64));
        //counties.put("TR", new CountyData("Teleorman", 3, 64));
        //counties.put("TM", new CountyData("Timiș", 3, 64));
        //counties.put("TL", new CountyData("Tulcea", 3, 64));
        //counties.put("VS", new CountyData("Vaslui", 3, 64));
        //counties.put("VL", new CountyData("Vâlcea", 3, 64));
        //counties.put("VN", new CountyData("Vrancea", 3, 64));
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
