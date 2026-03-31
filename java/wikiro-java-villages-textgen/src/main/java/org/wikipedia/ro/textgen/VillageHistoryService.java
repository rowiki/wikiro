
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
        buildCataPhrase(params, phrases);
        buildIdxPhrase(params, phrases);

        return phrases;
    }

    public String buildText(VillageHistoryParams params) {
        return buildPhrases(params).stream().collect(Collectors.joining("\n"));
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

            sb.append(".<ref>{{Citat Q|Q136757066");

            String bauerPage = params.getBauerPage();
            if (bauerPage != null) {
                sb.append("|p=").append(bauerPage);
            }

            sb.append("}}</ref>");
            phrases.add(sb.toString());
        }
    }

    private void buildSpechtPhrase(VillageHistoryParams params, List<String> phrases) {
        String spechtName = params.getSpechtName();
        if (spechtName != null && !spechtName.isEmpty()) {
            String spechtPhrase = "Pe harta Specht a Țării Românești din 1790, apare cu denumirea ''" + spechtName + "''.";
            String spechtRef = "<ref>{{Citat Q|Q136659961}}</ref>";
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
                sb.append(", având ").append(cataPop).append(" familii");
            }

            String cataFeci = params.getCataFeci();
            if (cataFeci != null && !cataFeci.isEmpty()) {
                sb.append(" și ").append(cataFeci).append(" feciori de muncă");
            }

            sb.append(".<ref>{{Citat Q|Q136354833");

            String cataPage = params.getCataPage();
            if (cataPage != null) {
                sb.append("|p=").append(cataPage);
            }

            sb.append("}}</ref>");
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

            sb.append(".<ref>{{Citat Q|Q136158772");

            String idx1954Page = params.getIdx1954Page();
            if (idx1954Page != null && !idx1954Page.isEmpty()) {
                sb.append("|p=").append(idx1954Page);
            }

            sb.append("}}</ref>");
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

            sb.append(".<ref>{{Citat Q|Q136158759");

            String idx1956Page = params.getIdx1956Page();
            if (idx1956Page != null && !idx1956Page.isEmpty()) {
                sb.append("|p=").append(idx1956Page);
            }

            sb.append("}}</ref>");
            phrases.add(sb.toString());
        }
    }
}
