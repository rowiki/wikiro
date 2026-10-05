
// src/main/java/org/wikipedia/ro/textgen/VillageHistoryService.java
package org.wikipedia.ro.textgen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VillageHistoryService {

    public List<String> buildPhrases(VillageHistoryParams params) {
        List<String> phrases = new ArrayList<>();

        buildBauerPhrase(params, phrases);
        buildSpechtPhrase(params, phrases);
        buildWeissPhrase(params, phrases);
        buildCataPhrase(params, phrases);
        buildVilComPhrase(params, phrases);
        buildOrgComPhrase(params, phrases);
        buildLegeComunalaPhrase(params, phrases);
        buildSatLegeComunalaPhrase(params, phrases);
        buildMdgrPhrase(params, phrases);
        buildIdxPhrase(params, phrases);

        return phrases;
    }

    public String buildText(VillageHistoryParams params) {
        return buildPhrases(params).stream().collect(Collectors.joining("\n"));
    }

    private void appendCaseFamiliiBiserici(StringBuilder sb, String caseCount, String familiiCount, String bisericiCount) {
        List<String> parts = new ArrayList<>();
        if (caseCount != null && !caseCount.isEmpty()) {
            parts.add(formatWithNumeralAgreement(caseCount, "case"));
        }
        if (familiiCount != null && !familiiCount.isEmpty()) {
            parts.add(formatWithNumeralAgreement(familiiCount, "familii"));
        }
        if (bisericiCount != null && !bisericiCount.isEmpty()) {
            parts.add(formatBisericiCount(bisericiCount));
        }
        if (parts.isEmpty()) {
            return;
        }
        sb.append(", având în total ");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(i == parts.size() - 1 ? " și " : ", ");
            }
            sb.append(parts.get(i));
        }
    }

    // Romanian numeral agreement: "de" is required unless the number's last two digits are 1..19.
    private String formatWithNumeralAgreement(String countStr, String noun) {
        try {
            int lastTwoDigits = Math.abs(Integer.parseInt(countStr.trim())) % 100;
            if (lastTwoDigits < 1 || lastTwoDigits > 19) {
                return countStr + " de " + noun;
            }
        } catch (NumberFormatException e) {
            // non-numeric value, fall through without agreement
        }
        return countStr + " " + noun;
    }

    private String formatBisericiCount(String countStr) {
        try {
            if (Integer.parseInt(countStr.trim()) == 1) {
                return "o biserică";
            }
        } catch (NumberFormatException e) {
            // non-numeric value, fall through to the regular plural form
        }
        return formatWithNumeralAgreement(countStr, "biserici");
    }

    private void buildBauerPhrase(VillageHistoryParams params, List<String> phrases) {
        String bauerName = params.getBauerName();
        if (bauerName != null && !bauerName.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Satul este menționat în memoriile generalului Bauer din 1778 ca ''").append(bauerName).append("''");

            String bauerDescription = params.getBauerDescription();
            if (bauerDescription != null) {
                sb.append(", drept ").append(bauerDescription);
            }

            sb.append(".{{RefQ|Q136757066");

            String bauerPage = params.getBauerPage();
            if (bauerPage != null) {
                sb.append("|p=").append(bauerPage);
            }

            sb.append("}}");
            phrases.add(sb.toString());
        }
    }

    private void buildSpechtPhrase(VillageHistoryParams params, List<String> phrases) {
        String spechtName = params.getSpechtName();
        if (spechtName != null && !spechtName.isEmpty()) {
            String spechtPhrase = "Pe harta Specht a Țării Românești din 1790, apare cu denumirea ''" + spechtName + "''.";
            String spechtRef = "{{RefQ|Q136659961}}";
            phrases.add(spechtPhrase + spechtRef);
        }
    }

    private void buildCataPhrase(VillageHistoryParams params, List<String> phrases) {
        String cataName = params.getCataName();
        if (cataName != null && !cataName.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Catagrafia din 1831 consemnează satul cu numele ''").append(cataName).append("''");

            String cataPlasa = params.getCataPlasa();
            if (cataPlasa != null) {
                sb.append(", în plasa ''").append(cataPlasa).append("''");
            }

            String cataCounty = params.getCataCounty();
            if (cataCounty != null) {
                sb.append(" din [[județul ").append(cataCounty);
                if (!"Secuieni".equalsIgnoreCase(cataCounty)) {
                    sb.append(" (interbelic)|");
                }
                sb.append("]]");
            }

            String cataMosie = params.getCataMosie();
            String cataOwner = params.getCataOwner();
            if (cataMosie != null && !cataMosie.isEmpty() || cataOwner != null && !cataOwner.isEmpty()) {
                sb.append(", pe moșia ");
                sb.append(cataMosie != null && !cataMosie.isEmpty() ? "''" + cataMosie + "'' " : "");
                if (cataOwner != null && !cataOwner.isEmpty()) {
                    sb.append(" deținută de ").append(cataOwner);
                }
            }

            String cataPop = params.getCataPop();
            if (cataPop != null && !cataPop.isEmpty()) {
                sb.append(", având ").append(formatWithNumeralAgreement(cataPop, "familii"));
            }

            String cataFeci = params.getCataFeci();
            if (cataFeci != null && !cataFeci.isEmpty()) {
                sb.append(" și ").append(formatWithNumeralAgreement(cataFeci, "feciori de muncă"));
            }

            sb.append(".{{RefQ|Q136354833");

            String cataPage = params.getCataPage();
            if (cataPage != null) {
                sb.append("|p=").append(cataPage);
            }

            sb.append("}}");
            phrases.add(sb.toString());
        }
    }

    private void buildIdxPhrase(VillageHistoryParams params, List<String> phrases) {
        String idx1954Name = params.getIdx1954Name();
        String idx1956Name = params.getIdx1956Name();

        if (idx1954Name != null && !idx1954Name.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Indicele localităților din 1954 menționează satul cu numele ''").append(idx1954Name).append("''");

            String idx1954Descr = params.getIdx1954Descr();
            if (idx1954Descr != null && !idx1954Descr.isEmpty()) {
                sb.append(", descriindu-l drept ").append(idx1954Descr);
            }

            sb.append(".{{RefQ|Q136158772");

            String idx1954Page = params.getIdx1954Page();
            if (idx1954Page != null && !idx1954Page.isEmpty()) {
                sb.append("|p=").append(idx1954Page);
            }

            sb.append("}}");
            phrases.add(sb.toString());

        }

        if (idx1956Name != null && !idx1956Name.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (idx1954Name != null && !idx1954Name.isEmpty()) {
                sb.append("Cel din 1956 îl menționează cu numele ''").append(idx1956Name).append("''");
            } else {
                sb.append("Indicele localităților din 1956 menționează satul cu numele ''").append(idx1956Name).append("''");
            }

            String idx1956Descr = params.getIdx1956Descr();
            if (idx1956Descr != null && !idx1956Descr.isEmpty()) {
                sb.append(',');
                if (idx1954Name != null && !idx1954Name.isEmpty()) {
                    sb.append(" descriindu-l");
                }
                sb.append(" drept ").append(idx1956Descr);
            }

            sb.append(".{{RefQ|Q136158759");

            String idx1956Page = params.getIdx1956Page();
            if (idx1956Page != null && !idx1956Page.isEmpty()) {
                sb.append("|p=").append(idx1956Page);
            }

            sb.append("}}");
            phrases.add(sb.toString());
        }
    }

    private void buildWeissPhrase(VillageHistoryParams params, List<String> phrases) {
        String weissName = params.getWeissName();
        if (weissName != null && !weissName.isEmpty()) {
            String phrase = "Harta Turciei Europene a lui Franz von Weiss din 1829 menționează satul cu numele ''" + weissName
                + "''.{{RefQ|Q136100477}}";
            phrases.add(phrase);
        }
    }

    private void buildMdgrPhrase(VillageHistoryParams params, List<String> phrases) {
        String mdgrName = params.getMdgrName();
        if (mdgrName == null || mdgrName.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("În Marele Dicționar Geografic al României, satul este menționat cu numele de ''").append(mdgrName).append("''");

        String commune = params.getMdgrCommune();
        boolean isResedinta = params.isMdgrIsResedinta();
        boolean communeSameName = params.isMdgrCommuneSameName();
        boolean sameCommune = params.isMdgrSameCommune();

        if (isResedinta) {
            if (communeSameName || commune == null || commune.isEmpty()) {
                sb.append(", ca reședința comunei cu același nume");
            } else {
                sb.append(", ca reședința comunei ''").append(commune).append("''");
            }
        } else if (sameCommune) {
            sb.append(", în aceeași comună");
        } else if (commune != null && !commune.isEmpty()) {
            sb.append(", în comuna ''").append(commune).append("''");
        }

        String plasa = params.getMdgrPlasa();
        if (params.isMdgrSamePlasa()) {
            sb.append(", aceeași plasă");
        } else if (plasa != null && !plasa.isEmpty()) {
            sb.append(", plasa ''").append(plasa).append("''");
        }

        String county = params.getMdgrCounty();
        if (params.isMdgrSameCounty()) {
            sb.append(", același județ");
        } else if (county != null && !county.isEmpty()) {
            sb.append(", județul ").append(county);
        }

        String pop = params.getMdgrPop();
        if (pop != null && !pop.isEmpty()) {
            sb.append(", având o populație de ").append(formatWithNumeralAgreement(pop, "locuitori"));
        }

        String extraDescription = params.getMdgrExtraDescription();
        if (extraDescription != null && !extraDescription.isEmpty()) {
            sb.append(". ").append(extraDescription);
        }

        String volume = params.getMdgrVolume();
        String refName = (volume != null && !volume.isEmpty()) ? "mdgr" + volume : "mdgr";
        sb.append(".<ref name=\"").append(refName).append("\">{{Citat MDGR|");
        if (volume != null && !volume.isEmpty()) {
            sb.append("|volum=").append(volume);
        }
        String page = params.getMdgrPage();
        if (page != null && !page.isEmpty()) {
            sb.append("|pagini=").append(page);
        }
        String refTitle = params.getMdgrRefTitle();
        if (refTitle != null && !refTitle.isEmpty()) {
            sb.append("|titlu=").append(refTitle);
        }
        sb.append("}}</ref>");

        phrases.add(sb.toString());
    }

    private void buildVilComPhrase(VillageHistoryParams params, List<String> phrases) {
        String comuna = params.getVilComComuna();
        if (comuna == null || comuna.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("În urma reformei administrative a lui Cuza din 1864, satele comunei făceau parte din comuna ").append(comuna);

        String plasa = params.getVilComPlasa();
        if (plasa != null && !plasa.isEmpty()) {
            sb.append(", plasa ").append(plasa);
        }

        String county = params.getVilComCounty();
        if (county != null && !county.isEmpty()) {
            sb.append(", [[județul ").append(county).append(" (interbelic)|]]");
        }

        appendCaseFamiliiBiserici(sb, params.getVilComCase(), params.getVilComFamilii(), params.getVilComBiserici());

        sb.append(".{{RefQ|Q140781961");
        String page = params.getVilComPage();
        if (page != null && !page.isEmpty()) {
            sb.append("|p=").append(page);
        }
        sb.append("}}");
        phrases.add(sb.toString());
    }

    private void buildOrgComPhrase(VillageHistoryParams params, List<String> phrases) {
        String comune = params.getOrgComComune();
        if (comune == null || comune.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("În urma reformei administrative a lui Cuza din 1864, pe teritoriul actual al comunei au fost organizate comunele ").append(comune);

        String plasa = params.getOrgComPlasa();
        if (plasa != null && !plasa.isEmpty()) {
            sb.append(" în plasa ").append(plasa);
        }

        String county = params.getOrgComCounty();
        if (county != null && !county.isEmpty()) {
            sb.append(" din [[județul ").append(county).append(" (interbelic)|]]");
        }

        appendCaseFamiliiBiserici(sb, params.getOrgComCase(), params.getOrgComFamilii(), params.getOrgComBiserici());

        sb.append(".{{RefQ|Q140781961");
        String page = params.getOrgComPage();
        if (page != null && !page.isEmpty()) {
            sb.append("|p=").append(page);
        }
        sb.append("}}");
        phrases.add(sb.toString());
    }

    private void buildLegeComunalaPhrase(VillageHistoryParams params, List<String> phrases) {
        String plasa = params.getLegeComunalaPlasa();
        String county = params.getLegeComunalaCounty();
        boolean singleVillage = params.isLegeComunalaSingleVillage();
        String villages = params.getLegeComunalaVillages();

        boolean hasCompunere = singleVillage || (villages != null && !villages.isEmpty());
        if ((plasa == null || plasa.isEmpty()) && (county == null || county.isEmpty()) && !hasCompunere) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Comuna a fost înființată prin legea comunală din 1864, în plasa ");
        if (plasa != null && !plasa.isEmpty()) {
            sb.append(plasa);
        }
        sb.append(" a [[Județul ");
        if (county != null && !county.isEmpty()) {
            sb.append(county);
        }
        sb.append(" (interbelic)|județului ");
        if (county != null && !county.isEmpty()) {
            sb.append(county);
        }
        sb.append("]], având în compunere ");
        sb.append(singleVillage ? "doar satul eponim" : "satele " + (villages != null ? villages : ""));

        String ext = params.getLegeComunalaExt();
        if (ext != null && !ext.isEmpty()) {
            sb.append(", pe teritoriul ei fiind identificate la acea dată și așezările ").append(ext);
        }

        appendCaseFamiliiBiserici(sb, params.getLegeComunalaCase(), params.getLegeComunalaFamilii(), params.getLegeComunalaBiserici());

        sb.append(".{{RefQ|Q140781961");
        String page = params.getLegeComunalaPage();
        if (page != null && !page.isEmpty()) {
            sb.append("|p=").append(page);
        }
        sb.append("}}");
        phrases.add(sb.toString());
    }

    private void buildSatLegeComunalaPhrase(VillageHistoryParams params, List<String> phrases) {
        boolean sameName = params.isSatLegeComunalaSameName();
        String commune = params.getSatLegeComunalaCommune();
        String plasa = params.getSatLegeComunalaPlasa();
        String county = params.getSatLegeComunalaCounty();

        if (!sameName && (commune == null || commune.isEmpty())
            && (plasa == null || plasa.isEmpty()) && (county == null || county.isEmpty())) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("În 1864, prin legea comunală, satul a ");
        sb.append(params.isSatLegeComunalaCapital() ? "devenit reședința comunei " : "fost arondat comunei ");
        sb.append(sameName ? "cu același nume" : "''" + (commune != null ? commune : "") + "''");
        sb.append(" din plasa ");
        if (plasa != null && !plasa.isEmpty()) {
            sb.append(plasa);
        }
        sb.append(", [[județul ");
        if (county != null && !county.isEmpty()) {
            sb.append(county);
        }
        sb.append(" (interbelic)|]]");

        sb.append(".{{RefQ|Q140781961");
        String page = params.getSatLegeComunalaPage();
        if (page != null && !page.isEmpty()) {
            sb.append("|p=").append(page);
        }
        sb.append("}}");
        phrases.add(sb.toString());
    }

}
