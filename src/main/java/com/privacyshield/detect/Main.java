package com.privacyshield.detect;

public class Main {
    public static void main(String[] args) {
        System.out.println(DataType.PHONE);
        for(DataType each : DataType.values()){
            System.out.println(each);
        }
        String text = "Call 9876543210 now";
        Detection d = new Detection(5, 15, DataType.PHONE, "9876543210");

        System.out.println(d);
        System.out.println(d.value());
        System.out.println(text.substring(d.start(), d.end()));

        PhoneDetector d = new PhoneDetector();
        String[] tests = {
                "Call 9876543210 now",
                "Call +91 98765 43210 now",
                "No: 5123456789",
                "Aadhaar 234567890123",
                "Two: 9876543210 8123456789"
        };
        for (String t : tests) {
            System.out.println(t + "  ->  " + d.detect(t));
        }
    }
}
