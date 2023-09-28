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

//    public DpResult runDp(Cid cid, byte[] data, String classFunctionName, Optional<Object[]> fnParams, Optional<String> constructorArgs) throws Exception {
//        // TODO, break out constructorConfigs in array
//        // But I cannot pass in an arbitary config file here, only seriliased properties which I determine types from
//        // Need another way to distinguish same typed objects, json/hashmap?
//        // Map[paramname, Object],
//        // For each argument, if it has an argument that is an object with its own constructors
//
//        return runDp(cid,data,classFunctionName,fnParams,)
//    }

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
            LOG.fine("Using SELECTIVE constructor");

//            //Object[] args = constructorArgs.get();
//            // TODO Match not the provide [] args to the constructor of right # args
//            //   but match arg types to provided list
//            // Need a way to match provided args to constructor
//            List<Class> argTypes = Arrays.stream(dpClass.getDeclaredConstructors())
//                    .filter(c -> c.getParameterTypes().length==args.length)
//                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
//                    .collect(Collectors.toList());
            // SO instead of above, getting the type from the constructor defn, I just take the json, infer the type (string, double, int) from the value
            // then assume there is a constructor

            /* extract from json, to a Map */
            Map<String,Object> argsMap = new Gson().fromJson(constructorArgs.get(), Map.class);
            LOG.fine("Args MAP: "+new Gson().toJson(argsMap));
            // What is the point of sending a json string, to only extract it into argtypes list and argsList?
//            List<Class> argTypes = argsMap.values().stream().map(v -> v.getClass()).collect(Collectors.toList());
//            //List<Object> args = (List<Object>) argsMap.values();
//            List<Object> argsList = argsMap.values().stream().map(a -> {
//                try {
//                    return parseObjectFromString(a.toString(), a.getClass());
//                } catch (Exception e) {e.printStackTrace();}
//                return null;
//            }).collect(Collectors.toList());
//
////            LOG.fine("# args in constructor: "+argTypes.size()+", args: "+new Gson().toJson(args));
////            Iterator<Class> argTypesIterator = argTypes.iterator();
////            Iterator<Object> argsIterator = args.iterator();
//////            Iterator<Object> argsIterator = Arrays.stream(args).iterator();
////            List<Object> argsList = new ArrayList<>();
////            while (argsIterator.hasNext() && argTypesIterator.hasNext()) {
////                Class argType = argTypesIterator.next();
////                Object newObj = parseObjectFromString(argsIterator.next().toString(), argType);
////                argsList.add(newObj);
////            }
//            LOG.fine("# arg types: "+argTypes.size()+", # args: "+argsList.size());
//            for (Class c : argTypes) {
//                LOG.fine("Class: "+c.getName());
//            }
//            for (Object o : argsList) {
//                LOG.fine("Arg: "+o.toString());
//            }
//            Object[] typedArgs = argsList.toArray(new Object[argsList.size()]);

            // TODO, if args provided dont have all the constructor args set them as optional.empty
            // Using the supplied json args (with key/value names), find the 'only' (by design) DP constructor which has all Optional args
            // If provided args json does not have a key/value for for one of the params, make it optional.empty
            // This is good because dont need to pass all the params over the network but can have general purpose flexibility
            Constructor dpConstructor = Arrays.stream(dpClass.getDeclaredConstructors()).filter(c -> c.getParameters().length>0).findFirst().get();
            List<Object> argsList = new ArrayList<>();
            Iterator<Class> paramTypesIt = Arrays.stream(dpConstructor.getParameterTypes()).iterator();
            for (String p : paranamer.lookupParameterNames(dpConstructor)) {
                LOG.fine("Param name: "+p+", args provided contains: "+argsMap.keySet().contains(p));
                if (argsMap.keySet().contains(p) && paramTypesIt.hasNext()) {
                    Object newObj = parseObjectFromString(argsMap.get(p).toString(), paramTypesIt.next());
                    argsList.add(newObj);
                } else {
                    argsList.add(null);
                }
            }
            LOG.info("ARgsList: "+new Gson().toJson(argsList));
            Object[] typedArgs = argsList.toArray(new Object[argsList.size()]);
            for (Object o : typedArgs) {
                LOG.info("Arg: "+o);
            }


            // TODO, see if can get the declared constructor whose parameters are the keys in the supplied map?
//            Constructor matchedConstructor = null;
//            Iterator argNamesIt = argsMap.keySet().iterator();
//            for (Constructor c : dpClass.getDeclaredConstructors()) {
//                boolean matches = true;
//                Parameter[] parameters = c.getParameters();
//                if (parameters.length!=argsMap.keySet().size()) {
//                    matches=false;
//                } else {
//                    for (Parameter p : parameters) {
//                        if (argNamesIt.hasNext() && !p.getName().equals(argNamesIt.next())) {
//                            matches = false;
//                            break;
//                        }
//                    }
//                    LOG.fine("Constructor: "+c.getName()+", Matches: "+matches);
//                    if (matches) {
//                        matchedConstructor = c;
//                    }
//                }
//            }
//            //instance = dpClass.getDeclaredConstructor(argTypes.toArray(new Class[argTypes.size()])).newInstance(typedArgs);
//            if (matchedConstructor==null) {
//                throw new IllegalArgumentException("No matching constructor");
//            }
//            instance = matchedConstructor.newInstance(typedArgs);
            instance = dpConstructor.newInstance(typedArgs);
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

