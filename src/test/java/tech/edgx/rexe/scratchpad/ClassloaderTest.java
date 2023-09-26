package tech.edgx.rexe.scratchpad;

import com.google.gson.Gson;
import io.ipfs.multihash.Multihash;
import org.junit.Before;
import org.junit.Test;
import tech.edgx.dp.mysqlcrud.model.User;
import tech.edgx.dp.chatsvc.model.Message;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;
import tech.edgx.rexe.util.DynamicClassLoader;
import tech.edgx.util.DpArgsMatcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
   Test demonstrates reading a jar file dynamically and running its methods
   - Need to decide if a DPJar should provide all of its capability in self-contained jvm/jar
     Or can inherit all the classes in the host DRF Node jVM
   - DPs need to import various libraries (sql-connectors, xmpp etc...) and might end up bloated/large
   - Might end up with the NODE imports various standardised libraries into JVM; then the DPs are constrained to which libraries
     they use; thus I might need a custom compiler and standards/specs for building compatible DPs
     DPs must use the correct libraries to function correctly; maybe import library groups; rexe.mysql-group; rexe.kafka-group etc...
   - Difference in the DRF will be I retrieve the jars directly from Content Addressed Storage,
     rather than reading from filesystem
   - Then I've got the ability to run the Http::EXEC referenced by unique CID, with functionName and params
     Load the procedure in the JVM, execute and return results as a DpResult
   - JVM must run with arg: -Djava.system.class.loader=tech.edgx.rexe.util.DynamicClassLoader - config in surefire plugin or as jvm cmd line arg
     Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
   - Must have mysql installed (due to dependence by various demo DPs). With user = (dp/dp)
 */
public class ClassloaderTest {

    static String TEST_USERNAME_A = "johno";
    static String TEST_USERNAME_B = "smithy";

    RexeClient rexeClient1;

    @Before
    public void setUp() throws Exception {
        /* Mock client required for runChatSvcDP() running dp/TestChatSvcDp.jar */
        rexeClient1 = mock(RexeClient.class);
        when(rexeClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.usercrud.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))), any())).thenReturn(
                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001").toJson()
        );
        when(rexeClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.usercrud.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))), any())).thenReturn(
                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001").toJson()
        );
    }

    @Test
    public void runJar() throws Exception {
        String jarFileName = "dp/TestDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        /* JVM must run with arg: -Djava.system.class.loader=tech.edgx.rexe.util.DynamicClassLoader - config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        // Run a Util Class method
        Class dpClass = Class.forName("tech.edgx.dp.testdp.DP", true, dcl);
        Helpers.printClassInfo(dpClass);

        Method method = dpClass.getDeclaredMethod("getTestVal", null);
        Object instance = dpClass.newInstance();
        Object result = method.invoke(instance);
        print("Result1: " + result);

        // Run Main class main() method
        Class dpClass2 = Class.forName("tech.edgx.dp.testdp.Main", true, dcl);
        for (Method method2 : dpClass2.getDeclaredMethods()) { // gets new methods programmed
            print("Declared Method: " + method2.toString());
        }
        Method method2 = dpClass2.getDeclaredMethod("main", String[].class);
        Object instance2 = dpClass2.newInstance();
        String[] params = new String[]{"testparam"};
        Object result2 = method2.invoke(instance2, new Object[]{params});
        print("Result2: " + result2);
    }

    @Test
    public void runMysqlJar() throws Exception {
        String jarFileName = "dp/TestMysqlDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        Class dpClass = Class.forName("tech.edgx.dp.mysqlcrud.DP", true, dcl);
        Helpers.printClassInfo(dpClass);
        Method insertMethod = dpClass.getDeclaredMethod("insert", String.class);
        Object instance = dpClass.newInstance();
        print("Result1: " + insertMethod.invoke(instance, new Object[]{"drftestuser"}));

        Method retrieveMethod = dpClass.getDeclaredMethod("retrieve", String.class);
        Helpers.printMethodInfo(retrieveMethod);

        Object result = retrieveMethod.invoke(instance, new Object[]{"drftestuser"});
        print("Result2: " + result);
        User user = (User) result;
        print("Result2: " + new Gson().toJson(user));

        String newEmail = "drftestuser.new@test.com";
        Method updateMethod = dpClass.getDeclaredMethod("update", String.class, String.class);
        print("Result3: " + updateMethod.invoke(instance, "drftestuser", newEmail));
        User user2 = (User) retrieveMethod.invoke(instance, new Object[]{"drftestuser"});
        print("Result3-1: " + new Gson().toJson(user2));

        Method deleteMethod = dpClass.getDeclaredMethod("delete", String.class);
        print("Result4: " + deleteMethod.invoke(instance, new Object[]{"drftestuser"}));
    }

    @Test
    public void runChatSvcDP() throws Exception {
        String jarFileName = "dp/TestChatSvcDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        Class dpClass = Class.forName("tech.edgx.dp.chatsvc.DP", true, dcl);
        Object instance = dpClass.getDeclaredConstructor(String.class).newInstance("/ip4/127.0.0.1/tcp/5001");

        Method queryFeedMethod = dpClass.getDeclaredMethod("queryFeed", Integer.class);

        // override client
        Method setDrfClientMethod = dpClass.getDeclaredMethod("overrideRexeClient", RexeClient.class);
        setDrfClientMethod.invoke(instance, rexeClient1);

        Object result = queryFeedMethod.invoke(instance, 5);
        List<Message> messages = (List<Message>) result;
        print("Result1: " + new Gson().toJson(messages));

        Method startChatMethod = dpClass.getDeclaredMethod("start", String.class, String.class);
        Object result2 = startChatMethod.invoke(instance, TEST_USERNAME_A, TEST_USERNAME_B);
        Integer chatId = (Integer) result2;
        print("Result2: " + chatId);
    }

    public void print(String message) {
        System.out.println(message);
    }
}
