package tech.edgx.drf.service;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import org.peergos.util.Logging;
import tech.edgx.drf.client.DrfClient;
import tech.edgx.drf.model.dp.DpResult;
import tech.edgx.drf.util.DpArgsMatcher;
import tech.edgx.dp.chatsvc.model.User;
import tech.edgx.drf.util.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
   THIS module is called the Distributed Execution Engine
   As an extension/companion of IPFS which is the Distributed Filesystem
   Together they provide DATA and NON-DATA Uniquely identified Resource Addressing and Distribution
   Other components will provide; PKI, user identity, priorities protocol and administrator App.
 */
public class RuntimeService {

    private static final Logger LOG = Logging.LOG();

    private static final String DP_CLASS_NAME = "DP";

    DynamicClassLoader dcl;
    //SomeClassLoader scl;
    //RemoteClassLoader rcl;
    //ByteClassLoader bcl;

    public RuntimeService() {

        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
        //scl = (SomeClassLoader) ClassLoader.getSystemClassLoader();
        //scl = new SomeClassLoader();
        //rcl = (RemoteClassLoader) ClassLoader.getSystemClassLoader();
        //bcl = ()
    }

    // Here the DP was pulled from FS (Store if local, else from p2p net); dpBytecode = store.get(cid).join().get();
    // Execute by loading the jar into VM using classloader, then execute the named function
    public DpResult runDp(Cid cid, byte[] data, String classFunctionName, Optional<Object[]> fnParams, Optional<Object[]> constructorArgs) throws Exception {
        LOG.info("Running DP, function: "+classFunctionName+", Params: "+(fnParams.isPresent() ? new Gson().toJson(fnParams.get()) : "Nil")+", bytecode length: "+data.length);

        /* V1 - write to file so that URLClassLoader can be used for dynamic loading
        *      todo, load direct from byte[], e.g.
        * https://www.programcreek.com/java-api-examples/?code=chenmudu%2FTomcat8-Source-Read%2FTomcat8-Source-Read-master%2Fapache-tomcat-8.5.49-src%2Fjava%2Forg%2Fapache%2Fcatalina%2Fwebresources%2FTomcatURLStreamHandlerFactory.java
        * https://stackoverflow.com/questions/17776884/any-way-to-create-a-url-from-a-byte-array
        * https://stackoverflow.com/questions/16602668/creating-a-classloader-to-load-a-jar-file-from-a-byte-array
        * https://stackoverflow.com/questions/16602668/creating-a-classloader-to-load-a-jar-file-from-a-byte-array#40004912
        * https://stackoverflow.com/questions/8938260/url-seturlstreamhandlerfactory#9253634
        */
        File jarFile = Files.write(Paths.get(cid.toBase58()+".dp"), data).toFile();
        Helpers.printJarInfo(jarFile);
        /* JVM must be running with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader or config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        URL url = jarFile.toURI().toURL();
        dcl.add(url);

        String functionName = classFunctionName.split(":")[1];
        // I THINK whatever is the class name, needs to be unique accross DPs, otherwise this will call the first DP.class that was loaded in the JVM
        //Class dpClass = null;
        //if (functionName.equals("start")) {

        Class dpClass = Class.forName(classFunctionName.split(":")[0], true, dcl);
//        } else {
//            dpClass = Class.forName(DP_CLASS_NAME, true, dcl);
//        }

        /// TEMP Override here
        if (classFunctionName.equals("tech.edgx.dp.chatsvc.DP:start")) {
            constructorArgs = Optional.of(new String[]{"aafkreidqsvifumsanj3etycgiluhj6hkiljswxdy73thpqmkwmrla6z24b"});
        }
        // TODO, if it has constructor, initialise with constructor
        //   Find the constructor arg types, then match provided args and call via e.g:
        //      Object instance = dpClass.getDeclaredConstructor(DrfClient.class).newInstance(drfClient1);
        // I'M TRYING TO FIGURE OUT if I should use the constructor here, or just have it that each function provides all the distributed computing capy
        // Is there a requirement to call the dp with multiple function calls per DP call??? if so then it makes sense
        // If I use the constructor appraoch is might also make testing mocks easier
        Object instance;
        if (constructorArgs.isPresent()) {
            LOG.info("Using SELECTIVE constructor");
            for (Constructor c : dpClass.getDeclaredConstructors()) {
                LOG.info("Constructor: "+c.getName()+", # args: "+c.getParameterTypes().length);
            }
            /// UP TO HERE: need to find the declared constructor that matches the # args/type provided
            List<Class> argTypes = Arrays.stream(dpClass.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toList());

            Object[] args = constructorArgs.get();
            LOG.info("# args in constructor: "+argTypes.size()+", # args provided: "+args.length);
            LOG.info("Prams: "+new Gson().toJson(args));
            Iterator<Class> argTypesIterator = argTypes.iterator();
            Iterator<Object> argsIterator = Arrays.stream(args).iterator();
            List<Object> __args = new ArrayList<>();
            while (argsIterator.hasNext() && argsIterator.hasNext()) {
                Object newObj = parseObjectFromString(argTypesIterator.next().toString(), argTypesIterator.next());
                __args.add(newObj);
            }
            LOG.info("Args: "+__args.size());
            Object[] myargs = __args.toArray(new Object[__args.size()]);
            LOG.info("Args: "+myargs.length);
            instance = dpClass.getDeclaredConstructor(argTypes.toArray(new Class[argTypes.size()])).newInstance(myargs);
        } else {
            LOG.info("Using default constructor");
            instance = dpClass.newInstance();
        }

        //dpClass.getDeclaredConstructors()
        //Object instance = dpClass.newInstance();
        for (Method m : dpClass.getDeclaredMethods()) {
            LOG.info("Method: " + m.toString());
            if (m.getName().contains("overrideDrfClient")) {
                //if (Helpers.isRunningTests()) {
                LOG.info("is Running Tests, will swap out DrfClient for a mock");
                String TEST_USERNAME_A = "drftestuserA";
                String TEST_USERNAME_B = "drftestuserB";
                /// TODO, TEMPORARILY - WHEN TESTING ONLY
                DrfClient drfClient1 = mock(DrfClient.class);
                when(drfClient1.compute(any(Multihash.class), any(), eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
                        new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A + " Fullname", TEST_USERNAME_A + "@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
                );
                when(drfClient1.compute(any(Multihash.class), any(), eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
                        new User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B + " Fullname", TEST_USERNAME_B + "@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
                );
                Method setDrfClientMethod = dpClass.getDeclaredMethod("overrideDrfClient", DrfClient.class);
                setDrfClientMethod.invoke(instance, drfClient1);
            }
        }

        Object result = null;
        if (fnParams.isPresent() ) {
            List<Class> parameterTypes = Arrays.stream(dpClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(functionName))
                    .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                    .collect(Collectors.toList());
            Object[] params = fnParams.get();
            LOG.info("# param in method: "+parameterTypes.size()+", # param provided: "+params.length);
            LOG.info("Prams: "+new Gson().toJson(params));
            Iterator<Class> paramTypesIterator = parameterTypes.iterator();
            Iterator<Object> paramsIterator = Arrays.stream(params).iterator();
            List<Object> __params = new ArrayList<>();
            while (paramTypesIterator.hasNext() && paramsIterator.hasNext()) {
                Object newObj = parseObjectFromString(paramsIterator.next().toString(), paramTypesIterator.next());
                __params.add(newObj);
            }
            LOG.info("Params: "+__params.size());
            Object[] myparams = __params.toArray(new Object[__params.size()]);
            LOG.info("Params: "+myparams.length);
            Method method = dpClass.getDeclaredMethod(functionName, parameterTypes.toArray(new Class[parameterTypes.size()]));
            //Class.forName("com.mysql.jdbc.Driver");
            result = method.invoke(instance, myparams);
            // Can throw java.lang.IllegalArgumentException: wrong number of arguments
        } else {
            Method method = dpClass.getDeclaredMethod(functionName);
            result = method.invoke(instance);
        }
        LOG.info("Result: "+result);
//        return new DpResult(cid, result.toString());
        return new DpResult(cid, result);
    }

    public static <T> T parseObjectFromString(String s, Class<T> clazz) throws Exception {
        return clazz.getConstructor(new Class[] {String.class }).newInstance(s);
    }
}

