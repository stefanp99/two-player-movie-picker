package com.andreea.twoplayermoviepicker.utils;

public class UtilityMethods {
    public static Boolean isSeedValid(String seed) {
        return seed != null && seed.matches("^[A-Z0-9]{4}$");
    }
}
