package me.pwnme.backend.Utils;

import me.pwnme.backend.Database.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class Utils {

    private static List<Character> chars = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
    private static char nullCharacter = '@';
    private static int separatorCharacter = 63;

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
        }
        return "0";
    }

    public static Long getBonusTimeFromToken(String token){

        long output = 0L;

        for(int i=0;i<token.length();i++)
            output += (int)token.charAt(i);

        return output;
    }

    public static PreparedStatement getPreparedStatement(String initialQuery, List<String> args) throws SQLException {
        initialQuery += " ";
        String[] parts = initialQuery.split("\\?");
        StringBuilder finalQuery = new StringBuilder();

        for(int i=0;i<args.size();i++)
            finalQuery.append(parts[i])/*.append("'")*/.append(args.get(i))/*.append("'")*/;

        finalQuery.append(parts[parts.length-1]);

        return Database.connection.prepareStatement(finalQuery.toString());

    }

    public static String craftToken(String arg0, String arg1, String arg2, String arg3){
        String token = "{\"email\": \"{1}\",\"timeCreated\": \"{2}\",\"timeExpire\": \"{3}\",\"password\": \"{4}\"}";

        token = token.replace("{1}", arg0);
        token = token.replace("{2}", arg1);
        token = token.replace("{3}", arg2);
        token = token.replace("{4}", arg3);

        return token;
    }

    public static String customEncode(String rawData){

        rawData = encodeBase64(rawData);

        List<Integer> var1 = new ArrayList<>();
        List<Integer> var2 = new ArrayList<>();
        List<Integer> var8 = new ArrayList<>();
        StringBuilder encodedData = new StringBuilder();
        int var4 = 0;
        int var5 = 0;


        rawData = rawData.replace("=", "");

        for(int i=0;i<rawData.length();i++)
            var1.add((int) rawData.charAt(i));

        for(int i=0;i<var1.size();i++){
            if(i==0)
                var2.add(var1.get(i) * var1.get(i+1));
            else if (i == var1.size()-1)
                var2.add(var1.get(i) * var1.get(i-1));
            else
                var2.add(var1.get(i-1) * var1.get(i) * var1.get(i+1));
        }

        for(Integer var6 : var2)
            var4 = Math.max(var4, var6);

        while(var4>chars.size()){
            var4 /= chars.size();
            var5++;
        }

        for(Integer var6 : var2){
            List<Integer> var7 = new ArrayList<>();
            while(var6>chars.size()){
                var7.add(var6%chars.size());
                var6/=chars.size();
            }

            for(int i=0;i<var5-var7.size();i++)
                var8.add((int) nullCharacter);

            Collections.reverse(var7);

            var8.add(var6);
            var8.addAll(var7);
        }

        for(Integer var9 : var8){
            if(var9 == 64)
                encodedData.append(nullCharacter);
            else
                encodedData.append(chars.get(var9));
        }

        return encodeBase64(String.valueOf(var5+1)) + (char) separatorCharacter + encodeBase64(String.valueOf((int)Math.pow(var1.get(0), var5+1))) + (char) separatorCharacter + encodedData;
    }


    public static String customDecode(String encodedData){

        String[] var1 = encodedData.split("\\?");
        int n = Integer.parseInt(decodeBase64(var1[0]));
        int firstData = Integer.parseInt(decodeBase64(var1[1]));
        String data = var1[2];
        ArrayList<Integer> var3 = new ArrayList<>();

        for(int i=0;i<data.length()/n;i++){
            int var4 = 0;
            for(int j=0;j<n;j++){
                char var5 = data.charAt(i * n + j);
                if(var5 != nullCharacter)
                    var4 += Math.pow(chars.size(), n-j-1) * chars.indexOf(var5);
            }
            var3.add(var4);
        }

        if(var3.size()<3)
            return "";

        ArrayList<Integer> var5 = new ArrayList<>();
        var5.add((int) Math.pow(firstData, 1.0/n));
        var5.add(var3.get(0)/var5.get(var5.size()-1));
        for(int i = 1;i<var3.size()-1;i++)
            var5.add(var3.get(i)/var5.get(var5.size()-1)/var5.get(var5.size()-2));

        StringBuilder output = new StringBuilder();

        for(int var6 : var5)
            output.append((char) var6);

        return decodeBase64(output.toString());

    }

}
