package org.wikipedia.ro.monuments.monuments_section;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.wikipedia.ro.monuments.monuments_section.data.Monument;

import com.mongodb.Block;

public class MonumentCollectorBlock implements Block<Document> {

    private List<Monument> collectorList;

    private static final Pattern LMI_CODE_PATTERN =
        Pattern.compile("([A-Z]{1,2})-([IV]+)-([sma])-([AB])-(\\d+)(?:\\.(\\d+))?");
    
    private static final Pattern SETTLEMENT_PATTERN = Pattern.compile("^(?:(?:sat\\s+)?\\s*(\\[\\[([^\\]\\|]*\\|)([^\\]\\|]+)\\]\\]\\s*;\\s*))?(comuna|oraș|municipiul)");

    public MonumentCollectorBlock(List<Monument> collectorList) {
        this.collectorList = collectorList;
    }

    public void apply(Document t) {
        Monument m = new Monument();
        m.dating = t.getString("Datare");
        m.name = t.getString("Denumire");
        m.code = t.getString("Cod");

        Matcher lmiCodeMatcher = LMI_CODE_PATTERN.matcher(m.code);
        if (lmiCodeMatcher.find()) {
            m.county = lmiCodeMatcher.group(1);
            String romanNum = lmiCodeMatcher.group(2);
            m.type = romanNum.indexOf('V') >= 0 ? 4 : romanNum.length();
            
            if (null == m.settlement) {
                m.settlement = t.getString("Localitate");
            }
            
            m.structure = lmiCodeMatcher.group(3).charAt(0);
            m.codeNumber = lmiCodeMatcher.group(5);
            m.supplementalCodeNumber = lmiCodeMatcher.group(6);
        }

        Matcher settlementMatcher = SETTLEMENT_PATTERN.matcher(t.getString("Localitate"));
        if (settlementMatcher.find()) {
            if (null != settlementMatcher.group(3) && 0 < settlementMatcher.group(3).trim().length()) {
                m.settlement = settlementMatcher.group(3).trim();
            }
        }

        collectorList.add(m);
    }

    public List<Monument> getCollectorList() {
        return collectorList;
    }

}
