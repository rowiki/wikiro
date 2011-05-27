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

     /** The original PDF that will be parsed. */
    public static final String pdf = "C:\\Users\\acipu.IXIACOM\\Desktop\\LMI-2010_AB.pdf";
    /** The resulting text file. */
    public static final String txt = "C:\\Users\\acipu.IXIACOM\\Desktop\\LMI-2010_AB.txt";
    
    private static String getCountyFullName() {
        int index = pdf.lastIndexOf("_") + 1;
        String countyCode = pdf.substring(index, index + 2);
        Hashtable counties = new Hashtable(); 
        counties.put("B.", "București");
        counties.put("AB", "Alba");
        counties.put("AR", "Arad");
        counties.put("AG", "Argeș");
        counties.put("BC", "Bacău");
        counties.put("BH", "Bihor");
        counties.put("BN", "Bistrița-Năsăud");
        counties.put("BT", "Botoșani");
        counties.put("BV", "Brașov");
        counties.put("BR", "Brăila");
        counties.put("BZ", "Buzău");
        counties.put("CS", "Caraș-Severin");
        counties.put("CL", "Călărași");
        counties.put("CJ", "Cluj");
        counties.put("CT", "Constanța");
        counties.put("CV", "Covasna");
        counties.put("DB", "Dâmbovița");
        counties.put("DJ", "Dolj"); 
        counties.put("GL", "Galați");
        counties.put("GR", "Giurgiu");
        counties.put("GJ", "Gorj"); 
        counties.put("HR", "Harghita");
        counties.put("HD", "Hunedoara");
        counties.put("IL", "Ialomița");
        counties.put("IS", "Iași");
        counties.put("IF", "Ilfov");
        counties.put("MM", "Maramureș");
        counties.put("MH", "Mehedinți");
        counties.put("MS", "Mureș");
        counties.put("NT", "Neamț");
        counties.put("OT", "Olt");
        counties.put("PH", "Prahova");
        counties.put("SM", "Satu Mare");
        counties.put("SJ", "Sălaj");
        counties.put("SB", "Sibiu");
        counties.put("SV", "Suceava");
        counties.put("TR", "Teleorman");
        counties.put("TM", "Timiș");
        counties.put("TL", "Tulcea");
        counties.put("VS", "Vaslui");
        counties.put("VL", "Vâlcea");
        counties.put("VN", "Vrancea");
        
        return (String)counties.get(countyCode);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
        throws DocumentException, IOException{
        PdfReader reader = new PdfReader(pdf);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        PrintStream out = new PrintStream(new FileOutputStream(txt), true, "UTF8");
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new MyTextExtractionStrategy(getCountyFullName()));
            out.println(strategy.getResultantText());
        }
        out.flush();
        out.close();
    }
}
