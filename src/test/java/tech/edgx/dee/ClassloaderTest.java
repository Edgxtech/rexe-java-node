package tech.edgx.dee;

import org.junit.Test;
import tech.edgx.dee.util.Helpers;
import tech.edgx.dee.util.DynamicClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

public class ClassloaderTest {

    /*
       Test demonstrates reading a jar file dynamically and running its methods
       - Need to decide if a DPJar should provide all of its capability in self-contained jvm/jar
         Or can inherit all the classes in the host DRF Node jVM
       - DPs need to import various libraries (sql-connectors, xmpp etc...) and might end up bloated/large
       - Might end up with the NODE imports various standardised libraries into JVM; then the DPs are constrained to which libraries
         they use; thus I might need a custom compiler and standards/specs for building compatible DPs
         DPs must use the correct libraries to function correctly; maybe import library groups; io.dee.mysql-group; io.dee.kafka-group etc...
       - Difference in the DRF will be I retrieve the jars directly from Content Addressed Storage,
         rather than reading from filesystem
       - Then I've go the ability to run the Http::EXEC referenced by unique CID, with functionName and params
         Load the procedure in the JVM, execute and return results as a DpResult
     */
    @Test
    public void runJar() throws Exception {
        String jarFileName = "src/main/resources/TestDp.jar";
        File jarFile = new File(jarFileName);
        Helpers.printJarInfo(jarFile);

        /* JVM must run with arg: -Djava.system.class.loader=util.tech.edgx.dee.DynamicClassLoader - config in surefire plugin */
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
    public void altRunJar() {
        // https://stackoverflow.com/questions/60764/how-to-load-jar-files-dynamically-at-runtime
//        URLClassLoader child = new URLClassLoader(
//                new URL[] {myJar.toURI().toURL()},
//                this.getClass().getClassLoader()
//        );
//        Class classToLoad = Class.forName("Main", true, child);
//        Method method = classToLoad.getDeclaredMethod("main");
//        Object instance = classToLoad.newInstance();
//        Object result = method.invoke(instance);
//        System.out.println("Result: "+result);


//        URL url = myJar.toURI().toURL();
//        URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
//        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//        method.setAccessible(true);
//        method.invoke(classLoader, url);

//        https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
//        DynamicClassLoader dcl = new DynamicClassLoader();
//        dcl.ad

//        JarClassLoader jcl = new JarClassLoader();
//        //Loading classes from different sources
//        jcl.add("DpTest.jar");
////        jcl.add(new URL("http://myserver.com/myjar.jar"));
////        jcl.add(new FileInputStream("myotherjar.jar"));
////        jcl.add("myclassfolder/");
//
//        //Recursively load all jar files in the folder/sub-folder(s)
//        //jcl.add("myjarlib/");
//        JclObjectFactory factory = JclObjectFactory.getInstance();
//        //Create object of loaded class
//        Object obj = factory.create(jcl, "Main");
    }
}
