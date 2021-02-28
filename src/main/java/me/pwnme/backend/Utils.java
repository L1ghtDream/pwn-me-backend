package me.pwnme.backend;

import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

public class Utils {

    //Crypto
    public static String encodeBase64(String rawData){
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

    public static String decodeBase64(String encodedData){
        return Arrays.toString(Base64.getDecoder().decode(encodedData.getBytes()));
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


    
}
