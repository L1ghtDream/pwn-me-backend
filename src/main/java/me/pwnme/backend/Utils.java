package me.pwnme.backend;

import java.util.*;

public class Utils {

    //Crypto
    public static String encodeBase64(String rawData){
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

    public static String decodeBase64(String encodedData){
        return new String(Base64.getDecoder().decode(encodedData));
    }

    public static String generateRandomString(int length) {

        StringBuilder output = new StringBuilder();

        for(int i=0;i<length;i++){
            int index;
            int chose = getRandomNumber(1,100);
            if(chose%2==0)
                index = getRandomNumber(65, 90);
            else
                index = getRandomNumber(97,112);
            output.append((char) index);
        }

        return output.toString();
    }

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static String checkForVulns(List<String> check){
        for(String str : check){
            if(str.contains("%"))
                return Response.string_format;
            if(str.contains(" "))
                return Response.sql_injection;
            if(str.equals(" "))
                return Response.null_or_empty_data;
        }
        return "0";
    }
}
