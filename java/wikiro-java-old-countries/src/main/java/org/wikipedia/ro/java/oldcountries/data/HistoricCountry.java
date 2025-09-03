package org.wikipedia.ro.java.oldcountries.data;

public enum HistoricCountry
{
    ROMANIA_CRT("Q218"),
    ROMANIA_RS("Q842794"),
    ROMANIA_RP("Q2685298"),
    ROMANIA_REGAT("Q203493"),
    ROMANIA_PRINCIPAT("Q958291"),
    TARA_ROMANEASCA("Q389004"),
    MOLDOVA("Q10957559"),
    IMP_OTOMAN("Q12560"),
    IMP_HABSBURG("Q153136"),
    IMP_AUSTRIA("Q131964"),
    AUSTRO_UNGARIA("Q28513"),
    UNGARIA_HORTHY("Q600018"),
    STAT_SR_HR_SL("Q15102440"),
    BANAT_REPUBLIC("Q156513"),
    UNGARIA_1REP("Q516160"),
    UNGARIA_EST_REGAT("Q625380"),
    TRANSILVANIA("Q655621"),
    UNGARIA_REGAT("Q16056854"),
    DOBRUJA_DESPOTATE("Q542165"),
    BULG_EMP2("Q420759"),
    BIZANTINE_EMP("Q12544"),
    BULG_EMP1("Q203817"),
    ROMAN_EMP("Q2277");
    
    
    private HistoricCountry(String qId)
    {
        this.qId = qId;
    }

    String qId;

    public String getqId()
    {
        return qId;
    }
    
    
}
