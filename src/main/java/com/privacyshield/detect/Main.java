package com.privacyshield.detect;

public class Main {
    public static void main(String[] args) {
        System.out.println(DataType.PHONE);
        for(DataType each : DataType.values()){
            System.out.println(each);
        }
    }
}
