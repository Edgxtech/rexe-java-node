package tech.edgx.drf.util;

import org.peergos.util.Logging;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
