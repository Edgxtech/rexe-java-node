package tech.edgx.rexe.service;

import com.google.gson.Gson;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.Paranamer;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.util.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DPExecutionService {

    private static final Logger LOG = Logging.LOG();

    DynamicClassLoader dcl;

    Paranamer paranamer = new AdaptiveParanamer();

    public DPExecutionService() {
        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
    }

    public DpResult runDp(Cid cid, byte[] data, String classFunctionName, Optional<Object[]> fnParams, Optional<String> constructorArgs) throws Exception { //Optional<Object[]> constructorArgs
        LOG.fine("Running DP, function: "+classFunctionName+", Params: "+(fnParams.isPresent() ? new Gson().toJson(fnParams.get()) : "Nil")+", bytecode length: "+data.length);
        /* V1 - write to file so that URLClassLoader can be used for dynamic loading, todo, load direct from byte[] */
        File jarFile = Files.write(Paths.get(cid.toBase58()+".dp"), data).toFile();
        URL url = jarFile.toURI().toURL();
        dcl.add(url);
        /* Simple protocol att to specify unique DP class & function */
        String functionName = classFunctionName.split(":")[1];
        Class dpClass = Class.forName(classFunctionName.split(":")[0], true, dcl);
        Object instance;
        /* if it has constructor, initialise with constructor */
        if (constructorArgs.isPresent()) {
            Map<String,Object> argsMap = new Gson().fromJson(constructorArgs.get(), Map.class);
            // Using the supplied json args (with key/value names), find the 'only' (by design) DP constructor which has all Optional args
            Constructor dpConstructor = Arrays.stream(dpClass.getDeclaredConstructors()).filter(c -> c.getParameters().length>0).findFirst().get();
            List<Object> argsList = new ArrayList<>();
            Iterator<Class> paramTypesIt = Arrays.stream(dpConstructor.getParameterTypes()).iterator();
            for (String p : paranamer.lookupParameterNames(dpConstructor)) {
                if (argsMap.keySet().contains(p) && paramTypesIt.hasNext()) {
                    Object newObj = parseObjectFromString(argsMap.get(p).toString(), paramTypesIt.next());
                    argsList.add(newObj);
                } else {
                    argsList.add(null);
                }
            }
            Object[] typedArgs = argsList.toArray(new Object[argsList.size()]);
            instance = dpConstructor.newInstance(typedArgs);
        } else {
            instance = dpClass.newInstance();
        }

        Object result = null;
        if (fnParams.isPresent() ) {
            List<Class> parameterTypes = Arrays.stream(dpClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(functionName))
                    .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                    .collect(Collectors.toList());
            Object[] params = fnParams.get();
            LOG.fine("# param in method: "+parameterTypes.size()+", params: "+new Gson().toJson(params));
            Iterator<Class> paramTypesIterator = parameterTypes.iterator();
            Iterator<Object> paramsIterator = Arrays.stream(params).iterator();
            List<Object> paramsList = new ArrayList<>();
            while (paramTypesIterator.hasNext() && paramsIterator.hasNext()) {
                Object newObj = parseObjectFromString(paramsIterator.next().toString(), paramTypesIterator.next());
                paramsList.add(newObj);
            }
            Object[] typedParams = paramsList.toArray(new Object[paramsList.size()]);
            Method method = dpClass.getDeclaredMethod(functionName, parameterTypes.toArray(new Class[parameterTypes.size()]));
            result = method.invoke(instance, typedParams);
            // Can throw java.lang.IllegalArgumentException: wrong number of arguments
        } else {
            Method method = dpClass.getDeclaredMethod(functionName);
            result = method.invoke(instance);
        }
        LOG.fine("DP Computation Result: "+result);
        return new DpResult(cid, result);
    }

    public static <T> T parseObjectFromString(String s, Class<T> clazz) throws Exception {
        return clazz.getConstructor(new Class[] {String.class }).newInstance(s);
    }
}

