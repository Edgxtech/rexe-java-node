package tech.edgx.rexe.service;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import org.peergos.util.Logging;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.util.*;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DistributedExecutionService {

    private static final Logger LOG = Logging.LOG();

    DynamicClassLoader dcl;

    public DistributedExecutionService() {
        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
    }

    public DpResult runDp(Cid cid, byte[] data, String classFunctionName, Optional<Object[]> fnParams, Optional<Object[]> constructorArgs) throws Exception {
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
            LOG.fine("Using SELECTIVE constructor");
            Object[] args = constructorArgs.get();
            List<Class> argTypes = Arrays.stream(dpClass.getDeclaredConstructors())
                    .filter(c -> c.getParameterTypes().length==args.length)
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toList());
            LOG.fine("# args in constructor: "+argTypes.size()+", args: "+new Gson().toJson(args));
            Iterator<Class> argTypesIterator = argTypes.iterator();
            Iterator<Object> argsIterator = Arrays.stream(args).iterator();
            List<Object> argsList = new ArrayList<>();
            while (argsIterator.hasNext() && argsIterator.hasNext()) {
                Class argType = argTypesIterator.next();
                Object newObj = parseObjectFromString(argsIterator.next().toString(), argType);
                argsList.add(newObj);
            }
            Object[] typedArgs = argsList.toArray(new Object[argsList.size()]);
            instance = dpClass.getDeclaredConstructor(argTypes.toArray(new Class[argTypes.size()])).newInstance(typedArgs);
        } else {
            LOG.fine("Using default constructor");
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

