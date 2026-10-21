package org.wikipedia.ro.villagesfixer.model;

@lombok.Data
public class DMSValue {
    private int degrees;
    private int minutes;
    private int seconds;

    public String toString() {
        return String.format("%d°%d'%d\"", degrees, minutes, seconds);
    }

    public static DMSValue fromDMS(String d, String m, String s) {
        DMSValue ret = new DMSValue();

        if (null != d && d.matches("\\d+")) {
            ret.degrees = Integer.parseInt(d);
        }
        if (null != m && m.matches("\\d+")) {
            ret.minutes = Integer.parseInt(m);
        }
        if (null != s && s.matches("\\d+")) {
            ret.seconds = Integer.parseInt(s);
        }
        return ret;
    }
}
