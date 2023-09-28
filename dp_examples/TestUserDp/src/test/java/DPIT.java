import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import tech.edgx.dp.usercrud.model.User;
import tech.edgx.rexe.client.RexeClient;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/* Must startup a cluster first,
  use: ./start.sh 0, ./start.sh 1, ./start.sh 2
  OR// a 'Multirun' config in IDE
*/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DPIT {

    static RexeClient client0;
    static RexeClient client1;
    static RexeClient client2;

    static Cid userDpCid;

    String TEST_USERNAME_A = "dave";
    String TEST_USER_NEW_EMAIL = "dave.new@test.com";

    @BeforeAll
    public static void setUp() {
        try {
            MultiAddress apiAddress0 = new MultiAddress("/ip4/127.0.0.1/tcp/5000");
            MultiAddress apiAddress1 = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
            MultiAddress apiAddress2 = new MultiAddress("/ip4/127.0.0.1/tcp/5002");
            client0 = new RexeClient(apiAddress0.getHost(), apiAddress0.getPort(), "/api/v0/", false);
            client1 = new RexeClient(apiAddress1.getHost(), apiAddress1.getPort(), "/api/v0/", false);
            client2 = new RexeClient(apiAddress2.getHost(), apiAddress2.getPort(), "/api/v0/", false);
            System.out.println("created clients");

            /* PRELOAD the User DP */
            String testDpName = "target/TestUserDp-1.0-SNAPSHOT-jar-with-dependencies.jar";
            File jarFile = new File(testDpName);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            System.out.println("# bytes1: " + bytecode.length);
            userDpCid = client0.put(bytecode, Optional.of("raw"));
            System.out.println("UserDP, b58: " + userDpCid.toBase58() + "; " + userDpCid.toString());
        }
        catch (Exception e) {e.printStackTrace();}
    }

    @Test
    @Order(1)
    public void testCreate() {
        String TEST_USERNAME_A = "dave";
        try {
            Object result1 = client0.compute(userDpCid, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
            print("DP compute result (create): " + result1);
            User user = User.fromJson((Map) result1);
            Assert.assertTrue("User created", TEST_USERNAME_A.equals(user.getUsername()) && user.getEmail().equals(TEST_USERNAME_A+"@test.com"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testRetrieve() throws Exception {
        Object result2 = client0.compute(userDpCid, Optional.empty(), "tech.edgx.dp.usercrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
        print("DP compute result (retrieve): " + result2);
        if (result2!=null) {
            User user = User.fromJson((Map) result2);
            print("Recovered user: " + new Gson().toJson(user));
            Assert.assertTrue("Created and retrieved object equal",
                    user.getUsername().equals(TEST_USERNAME_A)
                            && user.getEmail().equals(TEST_USERNAME_A + "@test.com")
                            && user.getFullname().equals(TEST_USERNAME_A + " Lastname"));
        } else {
            throw new AssertionError("User was null");
        }
    }

    @Test
    @Order(3)
    public void testUpdate() {
        String TEST_USERNAME_A = "dave";
        try {
            Object result1 = client0.compute(userDpCid, Optional.empty(), "tech.edgx.dp.usercrud.DP:update", Optional.of(new String[]{TEST_USERNAME_A, TEST_USER_NEW_EMAIL}), Optional.empty());
            print("DP compute result (update): " + result1);

            Object result2 = client0.compute(userDpCid, Optional.empty(), "tech.edgx.dp.usercrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
            print("DP compute result (retrieve): " + result2);
            User user = User.fromJson((Map) result2);
            print("Recovered user: "+new Gson().toJson(user));

            Assert.assertTrue("Created and retrieved object equal",
                    user.getUsername().equals(TEST_USERNAME_A)
                            && user.getEmail().equals(TEST_USERNAME_A+".new@test.com")
                            && user.getFullname().equals(TEST_USERNAME_A+" Lastname"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(4)
    public void testDelete() {
        String TEST_USERNAME_A = "dave";
        try {
            Object result1 = client0.compute(userDpCid, Optional.empty(), "tech.edgx.dp.usercrud.DP:delete", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
            print("DP compute result (delete): " + result1);
            Assert.assertTrue("Deleted",result1.equals("delete: ok"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void print(String msg) {
        System.out.println(msg);
    }

}
