package tech.edgx.drf.scratchpad;

import com.google.gson.Gson;
import io.ipfs.multihash.Multihash;
import org.junit.Before;
import org.junit.Test;
import tech.edgx.dp.mysqlcrud.model.User;
import tech.edgx.dp.chatsvc.model.Message;
import tech.edgx.drf.client.DrfClient;
import tech.edgx.drf.util.Helpers;
import tech.edgx.drf.util.DynamicClassLoader;
import tech.edgx.util.DpArgsMatcher;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassloaderTest {

    /*
       Test demonstrates reading a jar file dynamically and running its methods
       - Need to decide if a DPJar should provide all of its capability in self-contained jvm/jar
         Or can inherit all the classes in the host DRF Node jVM
       - DPs need to import various libraries (sql-connectors, xmpp etc...) and might end up bloated/large
       - Might end up with the NODE imports various standardised libraries into JVM; then the DPs are constrained to which libraries
         they use; thus I might need a custom compiler and standards/specs for building compatible DPs
         DPs must use the correct libraries to function correctly; maybe import library groups; io.drf.mysql-group; io.drf.kafka-group etc...
       - Difference in the DRF will be I retrieve the jars directly from Content Addressed Storage,
         rather than reading from filesystem
       - Then I've go the ability to run the Http::EXEC referenced by unique CID, with functionName and params
         Load the procedure in the JVM, execute and return results as a DpResult
       - JVM must run with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader - config in surefire plugin
         Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
     */

    static String TEST_USERNAME_A = "johno";
    static String TEST_USERNAME_B = "smithy";

    DrfClient drfClient1;

    @Before
    public void setUp() throws Exception {
        //MockitoAnnotations.initMocks(this);
        drfClient1 = mock(DrfClient.class);
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
        );
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
        );
    }

    @Test
    public void runJar() throws Exception {
        String jarFileName = "src/main/resources/TestDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        /* JVM must run with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader - config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        ///////////////////////////////////
        // Run a Util Class method
        ///////////////////////////////////
        Class dpClass = Class.forName("tech.edgx.dp.testdp.MyUtil", true, dcl);
        for (Method method : dpClass.getDeclaredMethods()) { // gets new methods programmed
            System.out.println("Declared Method: " + method.toString());
        }
        for (Method method : dpClass.getMethods()) { // gets all methods including native
            System.out.println("Method: " + method.toString());
        }
        Method method = dpClass.getDeclaredMethod("getTestVal", null);
        Object instance = dpClass.newInstance();
        Object result = method.invoke(instance);
        System.out.println("Result1: " + result);

        ///////////////////////////////////
        // Run Main class main() method
        ///////////////////////////////////
        Class dpClass2 = Class.forName("tech.edgx.dp.testdp.Main", true, dcl);
        for (Method method2 : dpClass2.getDeclaredMethods()) { // gets new methods programmed
            System.out.println("Declared Method: " + method2.toString());
        }
        Method method2 = dpClass2.getDeclaredMethod("main", String[].class);
        Object instance2 = dpClass2.newInstance();
        String[] params = new String[]{"testparam"};
        Object result2 = method2.invoke(instance2, new Object[]{params});
        System.out.println("Result2: " + result2);
    }

    @Test
    public void runMysqlJar() throws Exception {
        String jarFileName = "src/main/resources/TestMysqlDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        Class dpClass = Class.forName("tech.edgx.dp.mysqlcrud.DP", true, dcl);
        for (Method method : dpClass.getDeclaredMethods()) { // gets new methods programmed
            System.out.println("Declared Method: " + method.toString());
        }
        for (Method method : dpClass.getMethods()) { // gets all methods including native
            System.out.println("Method: " + method.toString());
        }
        Method insertMethod = dpClass.getDeclaredMethod("insert", String.class);
        Object instance = dpClass.newInstance();
        System.out.println("Result1: " + insertMethod.invoke(instance, new Object[]{"drftestuser"}));

        Method retrieveMethod = dpClass.getDeclaredMethod("retrieve", String.class);
        System.out.println(retrieveMethod.getReturnType());
        System.out.println(retrieveMethod.getAnnotatedReturnType());
        System.out.println(retrieveMethod.getReturnType().toGenericString());
        System.out.println(retrieveMethod.getReturnType().getDeclaredAnnotations());
        for (Annotation annot : retrieveMethod.getReturnType().getDeclaredAnnotations()) {
            System.out.println("annotation: "+annot.toString());
        }
        for (Method method : retrieveMethod.getReturnType().getDeclaredMethods()) {
            System.out.println("method: "+method.toString());
        }

        Object result = retrieveMethod.invoke(instance, new Object[]{"drftestuser"});
        System.out.println("Result2: " + result);
        User user = (User) result;
        System.out.println("Result2: " + new Gson().toJson(user));

        String newEmail = "drftestuser.new@test.com";
        Method updateMethod = dpClass.getDeclaredMethod("update", String.class, String.class);
        System.out.println("Result3: " + updateMethod.invoke(instance, "drftestuser", newEmail));
        User user2 = (User) retrieveMethod.invoke(instance, new Object[]{"drftestuser"});
        System.out.println("Result3-1: " + new Gson().toJson(user2));

        Method deleteMethod = dpClass.getDeclaredMethod("delete", String.class);
        System.out.println("Result4: " + deleteMethod.invoke(instance, new Object[]{"drftestuser"}));
    }

    @Test
    public void runChatSvcDP() throws Exception {
        String jarFileName = "src/main/resources/TestChatSvcDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        /* JVM must run with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader - config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        Class dpClass = Class.forName("tech.edgx.dp.chatsvc.DP", true, dcl);
        //Object instance = dpClass.newInstance(drfClient1); // If default constructor
        Object instance = dpClass.getDeclaredConstructor(String.class).newInstance("/ip4/127.0.0.1/tcp/5001"); //drfClient1

        Method method = dpClass.getDeclaredMethod("queryFeed", Integer.class);

        /// THEN HOW DO I SET THE DRFCLIENT AS MOCKED ONE?
        Method setDrfClientMethod = dpClass.getDeclaredMethod("overrideDrfClient", DrfClient.class);
        setDrfClientMethod.invoke(instance, drfClient1);

        Object result = method.invoke(instance, 5);
        List<Message> messages = (List<Message>) result;
        System.out.println("Result1: " + new Gson().toJson(messages));

        Method startChatMethod = dpClass.getDeclaredMethod("start", String.class, String.class); //String.class,
        Object result2 = startChatMethod.invoke(instance, TEST_USERNAME_A, TEST_USERNAME_B); //"bafkreidqsvifumsanj3etycgiluhj6hkiljswxdy73thpqmkwmrla6z24a",
        Integer chatId = (Integer) result2;
        System.out.println("Result2: " + chatId);
    }
}
