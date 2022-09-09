package org.wikipedia.ro.java.citation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils
{
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    
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

    public static String extractDate(String dateStr)
    {
        dateStr = StringUtils.substringBefore(dateStr, " ");
        try
        {
            OffsetDateTime publicationDateTime = OffsetDateTime.parse(dateStr);
            dateStr = DateTimeFormatter.ofPattern("uuuu-MM-dd").format(publicationDateTime);
        }
        catch (DateTimeParseException dpe)
        {
            LOG.debug("Could not extract date from string \"{}\"", dateStr, dpe);
        }
        return dateStr;
    }

}
