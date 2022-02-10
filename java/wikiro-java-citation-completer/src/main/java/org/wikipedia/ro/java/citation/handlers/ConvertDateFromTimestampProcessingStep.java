package org.wikipedia.ro.java.citation.handlers;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ConvertDateFromTimestampProcessingStep implements ProcessingStep
{

    private static final Pattern ALL_NUMBERS_PATTERN = Pattern.compile("\\d+");
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Europe/Bucharest"));

    @Override
    public Map<String, String> processParams(Map<String, String> params)
    {
        if (!params.containsKey("date"))
        {
            return params;
        }
        Matcher allNumbersMatcher = ALL_NUMBERS_PATTERN.matcher(StringUtils.trim(params.get("date")));
        if (allNumbersMatcher.matches())
        {
            Instant ts = Instant.ofEpochSecond(Long.parseLong(StringUtils.trim(params.get("date"))));
            params.put("date", DATE_FORMAT.format(ts));
        }
        return params;
    }

}
