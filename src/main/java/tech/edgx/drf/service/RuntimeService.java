package tech.edgx.drf.service;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.drf.model.dp.DpResult;
import tech.edgx.drf.util.DynamicClassLoader;
import tech.edgx.drf.util.Helpers;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    DynamicClassLoader dcl;

    public RuntimeService() {
        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
    }

    // Here the DP was pulled from FS (Store if local, else from p2p net); dpBytecode = store.get(cid).join().get();
    // Execute by loading the jar into VM using classloader, then execute the named function
    public DpResult runDp(Cid cid, byte[] data, String functionName, Optional<Object[]> optParams) throws Exception {
        LOG.info("Running DP, function: "+functionName+", Params: "+(optParams.isPresent() ? new Gson().toJson(optParams.get()) : "Nil"));
        File jarFile = Files.write(Paths.get("tmp.dp"), data).toFile();
        Helpers.printJarInfo(jarFile);
        /* JVM must be running with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader or config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        URL url = jarFile.toURI().toURL();
        dcl.add(url);
        Class dpClass = Class.forName(DP_CLASS_NAME, true, dcl);

        Object instance = dpClass.newInstance();
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

