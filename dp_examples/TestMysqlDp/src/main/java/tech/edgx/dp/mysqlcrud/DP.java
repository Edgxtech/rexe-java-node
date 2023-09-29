package tech.edgx.dp.mysqlcrud;

import tech.edgx.dp.mysqlcrud.model.User;

import java.sql.*;

/**
 * A demonstration mysql CRUD DP
 */
public class DP {

    public static String insert(String username) {
        String dbURL = "jdbc:mysql://localhost:3306/sampledb";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "INSERT INTO Users (username, password, fullname, email) VALUES (?, ?, ?, ?)";

            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, "secretpass");
            statement.setString(3, username+" Lastname");
            statement.setString(4, username+"@test.com");

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                return "insert: ok";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return "insert: no";
    }

    public static User retrieve(String username) {
        String dbURL = "jdbc:mysql://localhost:3306/sampledb";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "SELECT * From Users where username = ? order by user_id desc limit 1";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            User user = null;
            while (resultSet.next()) {
                user = new User(resultSet.getString("username"),
                                resultSet.getString("password"),
                        resultSet.getString("fullname"),
                        resultSet.getString("email"));
            }
            return user;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return null;
    }

    public static String update(String username, String newEmail) {
        String dbURL = "jdbc:mysql://localhost:3306/sampledb";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "UPDATE Users set email = ? where username = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, newEmail);
            statement.setString(2, username);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                return "update: ok";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return "update: no";
    }

    public static String delete(String username) {
        String dbURL = "jdbc:mysql://localhost:3306/sampledb";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "DELETE from Users where username = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, username);
            int rowsDeleted = statement.executeUpdate();
            if (rowsDeleted > 0) {
                return "delete: ok";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return "delete: no";
    }
}