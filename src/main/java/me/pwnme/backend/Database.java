package me.pwnme.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection connection;

    public static String host = "185.223.31.153";
    public static String database = "pwnme";
    public static String user = "pwnme";
    public static String password = "pwnme";
    public static int port = 3306;

    public static void sqlSetup() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&useSSL=false", host, port, database), user, password);
            System.out.println("Database connected!");
        } catch (SQLException | ClassNotFoundException var3) {
            var3.printStackTrace();
        }

    }
}
