package tech.edgx.drf.util_alternate;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.util.StreamUtils;


public class RemoteClassLoader extends ClassLoader {

    private byte[] jarBytes;
    private Set<String> names;

//    public RemoteClassLoader(byte[] jarBytes) throws IOException {
//        this.jarBytes = jarBytes;
//        this.names = RemoteClassLoader.loadNames(jarBytes);
//    }

    public RemoteClassLoader(String name, ClassLoader parent) {

        super(name, parent);
    }

    /*
     * Required when this classloader is used as the system classloader
     */
    public RemoteClassLoader(ClassLoader parent) {
        this("classpath", parent);
    }

    public RemoteClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }


    public void addJar(byte[] jarBytes) throws IOException {
        this.jarBytes = jarBytes;
        this.names = RemoteClassLoader.loadNames(jarBytes);
    }

    private static Set<String> loadNames(byte[] jarBytes) throws IOException {
        Set<String> set = new HashSet<>();
        try (ZipInputStream jis =
                     new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = jis.getNextEntry()) != null) {
                set.add(entry.getName());
            }
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz == null) {
            try {
                InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                StreamUtils.copy(in, out);
                byte[] bytes = out.toByteArray();
                clazz = defineClass(name, bytes, 0, bytes.length);
                if (resolve) {
                    resolveClass(clazz);
                }
            } catch (Exception e) {
                clazz = super.loadClass(name, resolve);
            }
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Check first if the entry name is known
        if (!names.contains(name)) {
            return null;
        }
        // I moved the JarInputStream declaration outside the
        // try-with-resources statement as it must not be closed otherwise
        // the returned InputStream won't be readable as already closed
        boolean found = false;
        ZipInputStream jis = null;
        try {
            jis = new ZipInputStream(new ByteArrayInputStream(jarBytes));
            ZipEntry entry;
            while ((entry = jis.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    found = true;
                    return jis;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Only close the stream if the entry could not be found
            if (jis != null && !found) {
                try {
                    jis.close();
                } catch (IOException e) {
                    // ignore me
                }
            }
        }
        return null;
    }
}