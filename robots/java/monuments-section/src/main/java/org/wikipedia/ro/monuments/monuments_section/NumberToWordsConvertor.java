package org.wikipedia.ro.monuments.monuments_section;

public class NumberToWordsConvertor {
    private int number;

    public NumberToWordsConvertor(int number) {
        super();
        this.number = number;
    }

    private static final String[] ORDERS = new String[] { "", "zeci", " sute" };
    private static final String[] DIGITS =
        new String[] { "zero", "un", "două", "trei", "patru", "cinci", "șase", "șapte", "opt", "nouă" };

    public String convert() {
        if (number < 10) {
            return DIGITS[number];
        }
        if (number == 10) {
            return "zece";
        }
        int lastDigit = number % 10;
        if (number < 20) {
            String prefix;
            switch (lastDigit) {
            case 1:
                prefix = "un";
                break;
            case 4:
                prefix = "pai";
                break;
            case 6:
                prefix = "șai";
                break;
            default:
                prefix = DIGITS[lastDigit];
            }
            return prefix + "sprezece";
        }
        if (number < 100) {
            return DIGITS[number / 10 % 10] + ORDERS[1] + (lastDigit > 0 ? (" și " + DIGITS[lastDigit]) : "");
        }
        return String.valueOf(number); //not implemented
    }
}
