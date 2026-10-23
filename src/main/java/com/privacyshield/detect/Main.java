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
    }
}
