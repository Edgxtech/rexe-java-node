package tech.edgx.drf.util_alternate;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.drf.model.dp.DpResult;
import tech.edgx.drf.util_alternate.DynamicClassLoader;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/*
   THIS module is called the Distributed Execution Engine
   As an extension/companion of IPFS which is the Distributed Filesystem
   Together they provide DATA and NON-DATA Uniquely identified Resource Addressing and Distribution
   Other components will provide; PKI, user identity, priorities protocol and administrator App.
 */
public class RuntimeService {

    private static final Logger LOG = Logging.LOG();

    private static final String DP_CLASS_NAME = "DP";

    tech.edgx.drf.util_alternate.DynamicClassLoader dcl;
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
    public DpResult runDp(Cid cid, byte[] data, String functionName, Optional<Object[]> optParams) throws Exception {
        LOG.info("Running DP, function: "+functionName+", Params: "+(optParams.isPresent() ? new Gson().toJson(optParams.get()) : "Nil")+", bytecode length: "+data.length);

        // V1
        //        File jarFile = Files.write(Paths.get("tmp"+Math.random()+".dp"), data).toFile();
//        Helpers.printJarInfo(jarFile);
//        /* JVM must be running with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader or config in surefire plugin */
//        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
//        URL url = jarFile.toURI().toURL();

//        ByteArrayInputStream bais = new ByteArrayInputStream(data);
//        bais.
        //URL url = new URL(null, "bytes///"+new String(data));

        //dcl.add(url);
        //scl = new SomeClassLoader(data);
//        BytecodeClassLoaderB bcl = new BytecodeClassLoaderB(data);
//        bcl.load();
        //dcl.setData(data)
        dcl.addBytecode(cid.toBase58(), data);
        //RemoteClassLoader rcl = new RemoteClassLoader(data);
        //rcl.addBytecode(data);

        //ByteClassLoader bcl = new ByteClassLoader(null)

        //rcl = new RemoteClassLoader(data);
        //rcl.addJar(data);

        //dcl.addJar(data);

        //Map<String,byte[]> extraClasses = new HashMap<>();
        //extraClasses.put("DP",data);
        //ByteClassLoader bcl = new ByteClassLoader(new URL[]{}, ClassLoader.getSystemClassLoader(), extraClasses);

        // I THINK whatever is the class name, needs to be unique accross DPs, otherwise this will call the first DP.class that was loaded in the JVM
        Class dpClass = null;
        if (functionName.equals("start")) {
            dpClass = Class.forName("tech.edgx.drf.dp.chatsvc.DP", true, dcl);
        } else {
            dpClass = Class.forName(DP_CLASS_NAME, true, dcl);
        }

        Object instance = dpClass.newInstance();
        for (Method m : dpClass.getDeclaredMethods()) {
            LOG.info("Method: "+m.toString());
        }

        Object result = null;
        if (optParams.isPresent() ) {
            List<Class> parameterTypes = Arrays.stream(dpClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(functionName))
                    .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                    .collect(Collectors.toList());
            Object[] params = optParams.get();
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

