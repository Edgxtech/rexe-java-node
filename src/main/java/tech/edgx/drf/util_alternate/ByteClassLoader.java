package tech.edgx.drf.util_alternate;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class ByteClassLoader extends URLClassLoader {
    private final Map<String, byte[]> extraClassDefs;

    public ByteClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
        super(urls, parent);
        this.extraClassDefs = new HashMap<String, byte[]>(extraClassDefs);
        //this.extraClassDefs = extraClassDefs;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        byte[] classBytes = this.extraClassDefs.remove(name);
        if (classBytes != null) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
    }

}