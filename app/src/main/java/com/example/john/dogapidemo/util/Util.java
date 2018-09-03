package com.example.john.dogapidemo.util;

public class Util {
    public static String capitalize(String word) {
        return String.valueOf(Character.toTitleCase(word.charAt(0))) +
                word.substring(1);
    }
}
