package tech.edgx.dee.service;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.util.DynamicClassLoader;
import tech.edgx.dee.util.Helpers;

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
        /* JVM must be running with arg: -Djava.system.class.loader=util.tech.edgx.dee.DynamicClassLoader - config in surefire plugin */
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
            Iterator<Class> paramTypesIterator = parameterTypes.iterator();
            Iterator<Object> paramsIterator = Arrays.stream(params).iterator();
            List<Object> __params = new ArrayList<>();
            while (paramTypesIterator.hasNext() && paramsIterator.hasNext()) {
                Object newObj = parseObjectFromString(paramsIterator.next().toString(), paramTypesIterator.next());
                __params.add(newObj);
            }
            Object[] myparams = __params.toArray(new Object[__params.size()]);
            Method method = dpClass.getDeclaredMethod(functionName, parameterTypes.toArray(new Class[parameterTypes.size()]));
            result = method.invoke(instance, myparams);
            // Can throws java.lang.IllegalArgumentException: wrong number of arguments
        } else {
            Method method = dpClass.getDeclaredMethod(functionName);
            result = method.invoke(instance);
        }
        return new DpResult(cid, result.toString());
    }

    public static <T> T parseObjectFromString(String s, Class<T> clazz) throws Exception {
        return clazz.getConstructor(new Class[] {String.class }).newInstance(s);
    }



//    public DpResult runDp(Cid cid, byte[] dp, String functionName, String[] params) throws Exception {
//        //String cmd = System.getProperty("user.dir")+System.getProperty("file.separator") + (!address.contains("service") ? EXTRACT_STAKE_ADDR_SCRIPT : EXTRACT_STAKE_ADDR_SCRIPT_TEST) + " " + address + " " + addressConversionBinary;
//        //String cmd = System.getProperty("user.dir")+System.getProperty("file.separator") + ;
//
//        Path dpPath = Path.of(System.getenv("HOME"), ".ipfs").resolve("dp").resolve("temp.jar");
//        LOG.fine("Writing file: "+new Gson().toJson(dpPath));
//        Files.write(dpPath, dp);
//
//        String[] cmd = { "/bin/sh", "-c", "java -jar temp.jar" };
//        LOG.fine("Running Command: "+cmd[2]);
//        Runtime rt = Runtime.getRuntime();
//        Process proc = rt.exec(cmd);
//        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//        String output = stdInput.readLine().trim();
//        LOG.fine("Dp execution result: "+output);
//        //return output;
//        return new DpResult(cid, output);
//    }

    public void runCommand(String cmd) throws Exception {
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd);
        try {
            pr.waitFor();
        } catch (InterruptedException ex) {
            LOG.severe("Error executing cmd: "+cmd);
        }
    }
}

