package tech.edgx.service;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.model.dp.DpResult;
import tech.edgx.util.DynamicClassLoader;
import tech.edgx.util.Helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class RuntimeService {

    private static final Logger LOG = Logging.LOG();

//    public DpResult execute(Cid cid, String functionName, String[] params) {
//        // bytecode was stored from a separate put
//        //   How to convert .jar file into byte[]?? -- the put block command simply takes a file thus can upload .jar
//        // Argument data is of file type. This endpoint expects one or several files (depending on the command) in the body of the request as 'multipart/form-data'.
//        // curl -X POST -F file=@myfile "http://127.0.0.1:5001/api/v0/block/put?cid-codec=raw&mhtype=sha2-256&mhlen=-1&pin=false&allow-big-block=false&format=<value>"
//
//        // Here need to pull the DP
//        byte[] dpBytecode = store.get(cid).join().get();
//        // How to convert it to a jar file for execution?  Or just load it straight into the VM and execute???
//        try {
//            // TODO, calc specific cid for the dp result? Or some way to hash the function and expected results
//            return new DpResult(cid, runtimeService.runDp(cid, dpBytecode));
//        } catch (Exception e) {
//            return null;
//        }
//    }
    DynamicClassLoader dcl;

    public RuntimeService() {
        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
    }

    // Here the DP was pulled from FS (Store if local, else from p2p net)
    // byte[] dpBytecode = store.get(cid).join().get();
    // Execute by loading the jar into VM using classloader, then execute the named function
    public DpResult runDp(Cid cid, byte[] dp, String functionName, Object[] params) throws Exception {
        File jarFile = Files.write(Paths.get("tmp.dp"), dp).toFile();
        Helpers.printJarInfo(jarFile);
        /* JVM must be running with arg: -Djava.system.class.loader=tech.edgx.util.DynamicClassLoader - config in surefire plugin */
        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
        URL url = jarFile.toURI().toURL();
        dcl.add(url);
        Class dpClass = Class.forName("Dp", true, dcl);
        Method method = dpClass.getDeclaredMethod(functionName, Object[].class);
        Object instance = dpClass.newInstance();
        // TODO, Might need to pass in the output model
        Object result = method.invoke(instance, new Object[]{params});
        return new DpResult(cid, result.toString());
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

