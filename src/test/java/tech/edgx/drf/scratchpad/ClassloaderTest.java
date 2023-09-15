package tech.edgx.drf.scratchpad;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import tech.edgx.drf.model.User;
import tech.edgx.drf.util.Helpers;
import tech.edgx.drf.util.DynamicClassLoader;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.net.URL;

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
        Class dpClass = Class.forName("MyUtil", true, dcl);
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
        Class dpClass2 = Class.forName("Main", true, dcl);
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

        Class dpClass = Class.forName("DP", true, dcl);
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
}
