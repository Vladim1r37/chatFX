package server;

import java.sql.*;
import java.util.ArrayList;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickname(String login, String pass) {
        String query = String.format("select nickname from main\n" +
                "where login = '%s'\n" +
                "and password = '%s'", login, pass);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean signUp(String login, String password, String nickname) {
        String query = String.format("select id from main\n" +
                "where login = '%s'\n" +
                "or nickname = '%s'", login, nickname);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return false;
            }
            query = String.format("insert into main (login, password, nickname)\n" +
                    "values ('%s', '%s', '%s')\n", login, password, nickname);
            stmt.executeUpdate(query);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
