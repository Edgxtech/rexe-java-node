package tech.edgx.dp.usercrud;

import tech.edgx.dp.usercrud.util.HexUtil;
import tech.edgx.dp.usercrud.model.User;

import java.security.*;
import java.sql.*;

/**
 * A demonstration user DP
 *
 */
public class DP {

    static String dbURL = "jdbc:mysql://localhost:3306/userdb";

    public static User create(String username) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(512, random);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey priv = pair.getPrivate();
            PublicKey pub = pair.getPublic();
            System.out.println("Generated pubkey: "+ HexUtil.encodeHexString(pub.getEncoded()));

            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "INSERT INTO Users (uname, password, fullname, email, pubkey) VALUES (?, ?, ?, ?, ?)";

            User user = new User(username, "secretpass",username+" Lastname",username+"@test.com", HexUtil.encodeHexString(pub.getEncoded()));
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFullname());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getPubkey());

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                return user;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User retrieve(String username) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "SELECT * From Users where uname = ? order by user_id desc limit 1";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            User user = null;
            if (resultSet.next()) {
                user = new User(resultSet.getString("uname"),
                                resultSet.getString("password"),
                                resultSet.getString("fullname"),
                                resultSet.getString("email"),
                                resultSet.getString("pubkey"));
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
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "UPDATE Users set email = ? where uname = ?";
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
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "DELETE from Users where uname = ?";
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