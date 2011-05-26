/*
 * To change this template, choose Tools | Templates
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

/**
 *
 * @author acipu
 */
public class PdfTestExtractor {

     /** The original PDF that will be parsed. */
    public static final String pdf = "C:\\Users\\andrei\\Desktop\\LMI\\LMI-2010_AB.pdf";
    /** The resulting text file. */
    public static final String txt = "C:\\Users\\andrei\\Desktop\\LMI\\LMI-2010_AB.txt";
    
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
            strategy = parser.processContent(i, new MyTextExtractionStrategy());
            out.println(strategy.getResultantText() + "\n");
        }
        out.flush();
        out.close();
    }
}
