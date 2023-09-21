package tech.edgx.drf.util_alternate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.*;

public class BytecodeClassLoaderB {

    //private static final byte[] jarBytes = new byte[] { 0x00 /* .... etc*/ };
    private static byte[] jarBytes;
    DynamicClassLoader dcl;

    public BytecodeClassLoaderB(byte[] data) {
        this.jarBytes = data;
        dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
    }

    //public static void main(String[] args) throws Exception {
    public void load() throws Exception {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String urlProtocol) {
                System.out.println("Someone asked for protocol: " + urlProtocol);
                if ("myjarprotocol".equalsIgnoreCase(urlProtocol)) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) throws IOException {
                            return new URLConnection(url) {
                                public void connect() throws IOException {}
                                public InputStream getInputStream() throws IOException {
                                    System.out.println("Someone is getting my jar!!");
                                    return new ByteArrayInputStream(jarBytes);
                                }
                            };
                        }
                    };
                }
                return null;
            }
        });

        System.out.println("Loading jar with fake protocol!");
        loadJarFromURL(new URL("myjarprotocol:fakeparameter"));
    }

    public static final void loadJarFromURL(URL jarURL) throws Exception {
        //URLClassLoader systemClassloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        DynamicClassLoader dcl = (DynamicClassLoader) ClassLoader.getSystemClassLoader();

//                File jarFile = Files.write(Paths.get("tmp"+Math.random()+".dp"), data).toFile();
//        Helpers.printJarInfo(jarFile);
//        /* JVM must be running with arg: -Djava.system.class.loader=util.tech.edgx.drf.DynamicClassLoader or config in surefire plugin */
//        // Based on: https://github.com/update4j/update4j/blob/master/src/main/java/org/update4j/DynamicClassLoader.java
//        URL url = jarFile.toURI().toURL();

//        Method systemClassloaderMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//        systemClassloaderMethod.setAccessible(true);
//        systemClassloaderMethod.invoke(dcl, jarURL);
        dcl.add(jarURL);

        // This make classloader open the connection
        dcl.findResource("/resource-404");
    }

}