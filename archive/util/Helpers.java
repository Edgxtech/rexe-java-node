package tech.edgx.dee.util;

import org.peergos.util.Logging;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.Iterator;
import java.util.logging.Logger;

public class Helpers {

    private static final Logger LOG = Logging.LOG();

    public static void printJarInfo(File file) throws IOException {
        if (file.exists()){
            JarFile jarfile = new JarFile(file);
            Manifest manifest = jarfile.getManifest();
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
}