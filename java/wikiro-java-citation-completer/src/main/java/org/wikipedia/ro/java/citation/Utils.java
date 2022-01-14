package org.wikipedia.ro.java.citation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Utils
{
    private Utils() {
        
    }
    
    public static String encodeURIComponent(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
        }
        catch (UnsupportedEncodingException e)
        {
            return s;
        }
    }

    public static String extractDate(String publicationDate)
    {
        try
        {
            OffsetDateTime publicationDateTime = OffsetDateTime.parse(publicationDate);
            publicationDate = DateTimeFormatter.ofPattern("uuuu-MM-dd").format(publicationDateTime);
        }
        catch (DateTimeParseException dpe)
        {
            dpe.printStackTrace();
        }
        return publicationDate;
    }

}
