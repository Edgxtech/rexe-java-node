package tech.edgx.dp.chatsvc;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.dp.chatsvc.model.Message;
import tech.edgx.dp.usercrud.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

/**
 * Chat service DP for demonstration purposes. Not a recommended solution.
 *    /* Workflows:
 *           - initiate a chat with list of users for the group
 *           - Make use of other DPs/Data Resources:
 *                 UserCRUD DP,
 *                 file resources (images, etc..)
 *                 Arbitrary DP as required such as social network
 *            - Security, privacy, registration, authentication, authorisation and encryption, black data mgt and access not covered yet. For now, a user is
 *                 entirely managed by the app developer, and any auth with the resource needs to be added such as access token issuance.
 *                 Later this is intended to be facilitated by an organic PKI within the DRF as an intrinsic network wide capability
 *            - Should it make use of XMPP?
 *                 - Or perhaps this is a separate/alternative DP : XMPP Wrapper DP, leave for now
 *                   Since goal here is to demonstrate reusing composable DPs from DPs of which UserCRUD is the demonstration used
 *        calls: start(), send(), queryFeed()
 *               start(user[] users); authorisation -> chatsDB.create() {required jwt scopes?, users, user access tokens, data protection, websocket/socket allocation?, partition db entities}
 *               send; user authorisation -> chatsDB.updatechatfeed() {add to db, notify users}
 *               queryFeed; {retrieve message history}
 *        app: provides UI to start chats, manage contacts, send messages and view chat history
 */
public class DP {
    private static final Logger LOG = Logger.getLogger(DP.class.getName());

    static String dbURL = "jdbc:mysql://localhost:3306/chatdb";

    public RexeClient rexeClient;
    public String userDpHash;

    // TODO, find a way to check localhost for the port being used by a running DRF node
    /* Defaults are the local host and a known deployed UserCRUD DP */
    private String DEFAULT_USER_DP = "bafkreidqsvifumsanj3etycgiluhj6hkiljswxdy73thpqmkwmrla6z24a";
    private String DEFAULT_CLIENT_API = "/ip4/127.0.0.1/tcp/5001";

    // For injecting mock client in testing,
    public void overrideRexeClient(RexeClient rexeClient) {
        this.rexeClient = rexeClient;
    }

    public DP() {
        MultiAddress drfClientAddress = new MultiAddress(DEFAULT_CLIENT_API);
        this.rexeClient = new RexeClient(drfClientAddress.getHost(), drfClientAddress.getPort(), "/api/v0/", false);
        this.userDpHash = DEFAULT_USER_DP;
    }

    // DP Execution Engine allows for a single non-default constructor only, nulls must be passed for unused optional parameters.
    // Parameter definition must be available to app developers and are required to make successful calls
    public DP(String apiUrl, String userDpHash) {
        MultiAddress drfClientAddress = apiUrl!=null ? new MultiAddress(apiUrl) : new MultiAddress(DEFAULT_CLIENT_API);
        this.rexeClient = new RexeClient(drfClientAddress.getHost(), drfClientAddress.getPort(), "/api/v0/", false);
        this.userDpHash = userDpHash != null ? userDpHash : DEFAULT_USER_DP;
        LOG.fine("Constructed using client url: "+drfClientAddress.getHost()+":"+drfClientAddress.getPort()+", userDpHash: "+userDpHash);
    }

    public User retrieveUser(String username) throws Exception {
        LOG.fine("Retrieving user: "+username+", via resource: "+userDpHash+", drfClient: "+ rexeClient.host+":"+ rexeClient.port);
        return User.fromJson((Map) rexeClient.compute(Cid.decode(userDpHash), Optional.empty(), "tech.edgx.dp.usercrud.DP:retrieve", Optional.of(new String[]{username}), Optional.empty()));
    }

    public Integer start(String creatorUsername, String requestedUsername) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");

            String sql_chat = "INSERT INTO chat (date_created) values (now())";
            PreparedStatement statement = conn.prepareStatement(sql_chat, RETURN_GENERATED_KEYS);
            statement.executeUpdate();
            ResultSet rs = statement.getGeneratedKeys();
            Integer chatId = rs.next() ? rs.getInt(1) : null;
            LOG.fine("Inserted chat, id: "+chatId);

            if (chatId != null) {
                /* Lookup user via separate DP containing user data */
                User creatorUser = retrieveUser(creatorUsername);
                User requestedUser = retrieveUser(requestedUsername);
                LOG.fine("Creator user: "+new Gson().toJson(creatorUser)+", Requested user: "+new Gson().toJson(requestedUser));

                // By arbitrary design, the app (chatSvc) DP here chooses to make another user table that holds ref to global User DP
                String sql_chat_user = "INSERT INTO chat_user (uname,chat_id,pubkey) values (?,?,?)";
                statement = conn.prepareStatement(sql_chat_user, RETURN_GENERATED_KEYS);
                statement.setString(1, creatorUsername);
                statement.setInt(2, chatId);
                statement.setString(3, creatorUser.getPubkey());
                statement.executeUpdate();

                statement = conn.prepareStatement(sql_chat_user, RETURN_GENERATED_KEYS);
                statement.setString(1, requestedUsername);
                statement.setInt(2, chatId);
                statement.setString(3, requestedUser.getPubkey());
                statement.executeUpdate();
            }
            return chatId;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String send(Integer chatId, String creator_username, String content) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");

            String sql = "INSERT INTO MESSAGE (chat_id,creator_uname,content,dtg) values (?,?,?,now())";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, chatId);
            statement.setString(2, creator_username);
            statement.setString(3, content);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                return "insert message: ok";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return "insert message: no";
    }

    public List<Message> queryFeed(Integer chatId) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbURL, "dp", "dp");
            String sql = "SELECT * from MESSAGE where CHAT_ID = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            List<Message> messages = new ArrayList<>();
            while (resultSet.next()) {
                messages.add(new Message(resultSet.getDate("dtg"),
                        resultSet.getString("creator_uname"),
                        resultSet.getString("content")));
            }
            return messages;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return new ArrayList<>();
    }
}