package tech.edgx.rexe.util;

import org.peergos.util.Logging;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.Iterator;
import java.util.logging.Logger;

public class Helpers {

    private static final Logger LOG = Logging.LOG();

    public static void printJarInfo(File file) throws IOException {
        if (file.exists()){
            JarFile jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = (Attributes)manifest.getMainAttributes();
            for (Iterator it=attrs.keySet().iterator(); it.hasNext(); ) {
                Attributes.Name attrName = (Attributes.Name)it.next();
                String attrValue = attrs.getValue(attrName);
                LOG.info(attrName + ": " + attrValue);
            }
        }
        else{
            LOG.fine("File not found.");
        }
    }

    public static void printClassInfo(Class theClass) {
        for (Method method : theClass.getDeclaredMethods()) {
            LOG.info("Declared Method: " + method.toString());
        }
        for (Method method : theClass.getMethods()) {
            LOG.info("Method: " + method.toString());
        }
    }

    public static void printMethodInfo(Method method) {
        for (Annotation annot : method.getReturnType().getDeclaredAnnotations()) {
            LOG.info("annotation: "+annot.toString());
        }
        for (Method m : method.getReturnType().getDeclaredMethods()) {
            LOG.info("method: "+method.toString());
        }
    }

    public static boolean isRunningTests() {
        for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
            LOG.info("StackEle: "+s.getClassName());
            if (s.getClassName().contains("org.junit.runners.model")) {
                return true;
            }
        }
        return false;
    }
}
