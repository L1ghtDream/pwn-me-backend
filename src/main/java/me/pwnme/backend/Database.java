package me.pwnme.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection connection;

    public static void sqlSetup() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            //connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&useSSL=false", credentials.host, port, database), user, pass);
            System.out.println("Database connected!");
        } catch (SQLException | ClassNotFoundException var3) {
            var3.printStackTrace();
        }

    }
}
