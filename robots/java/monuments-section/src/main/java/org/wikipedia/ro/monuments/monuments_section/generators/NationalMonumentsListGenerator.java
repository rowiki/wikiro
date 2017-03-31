package org.wikipedia.ro.monuments.monuments_section.generators;

import static org.wikipedia.ro.monuments.monuments_section.Utils.*;

import java.util.List;

import org.wikipedia.ro.monuments.monuments_section.NumberToWordsConvertor;
import org.wikipedia.ro.monuments.monuments_section.data.Monument;

public class NationalMonumentsListGenerator extends AbstractMonumentGenerator {

    @Override
    public String generate(List<Monument> monList) {
        List<List<Monument>> splitMonuments = splitMonumentsByType(monList);
        if (splitMonuments.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (splitMonuments.size() == 1) { // only one type of monuments
            if (splitMonuments.get(0).size() == 1) { // only one monument total
                Monument theMonument = splitMonuments.get(0).get(0);
                sb.append(MONUMENT_TYPE_DESCRIPTIONS[theMonument.type][1]).append(" de interes național ");
                describeNameAndDatingLong(sb, theMonument);
                describeEnsemble(sb, theMonument);
            } else { // more monuments of only one type
                sb.append(new NumberToWordsConvertor(splitMonuments.get(0).size()).convert()).append(' ')
                    .append(MONUMENT_TYPE_DESCRIPTIONS[splitMonuments.get(0).get(0).type][2])
                    .append(" de interes național: ");
                List<String> monumentDescriptions = generateMonumentsListDescription(splitMonuments.get(0));
                sb.append(joinWithConjunction(monumentDescriptions, ", ", " și "));
            }
        } else { // more types of monuments
            String introWordSingle = "Unul";
            String introWordMultiple = "";
            sb.append(qualifyinglyPluralizeByGrammarSet(monList.size(), MONUMENT_TYPE_DESCRIPTIONS[0]))
                .append(" de interes național").append(". ");
            for (List<Monument> eachMonumentTypeList : splitMonuments) {
                if (1 == eachMonumentTypeList.size()) {
                    sb.append(introWordSingle).append(" este ")
                        .append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][1]).append(' ');

                } else {
                    String countInWords = new NumberToWordsConvertor(eachMonumentTypeList.size()).convert();
                    if (introWordMultiple.length() > 0) {
                        sb.append(introWordMultiple).append(' ').append(countInWords);
                    } else {
                        sb.append(capitalize(countInWords));
                    }
                    sb.append(" sunt ").append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][3]).append(' ');
                }
                List<String> monumentDescriptions = generateMonumentsListDescription(eachMonumentTypeList);
                sb.append(joinWithConjunction(monumentDescriptions, "; ", "; și ")).append(". ");
                introWordSingle = "Altul";
                introWordMultiple = "Alte";
            }
        }

        return sb.toString();
    }

}
