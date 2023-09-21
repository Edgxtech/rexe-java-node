package tech.edgx.drf.util_alternate;

/*
 * Copyright 2018 Mordechai Meisels
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/* -Djava.system.class.loader=tech.edgx.drf.util.DynamicClassLoader */
public final class DynamicClassLoader extends URLClassLoader {

//    private byte[] jarBytes;
//    private Set<String> names;

    private final Map<String, byte[]> entries = new HashMap<>();

    static {
        registerAsParallelCapable();
    }

//    public byte[] data;
//
//    public void setData(byte[] data) {
//        this.data = data;
//    }

    //URLStreamHandlerFactory factory;

    public DynamicClassLoader(String name, ClassLoader parent) {
        super(name, new URL[0], parent);

//        try {
//            URLStreamHandlerFactory factory1 = new URLStreamHandlerFactory() {
//                public URLStreamHandler createURLStreamHandler(String urlProtocol) {
//                    //System.out.println("Someone asked for protocol: " + urlProtocol);
//                    if ("myjarprotocol".equalsIgnoreCase(urlProtocol)) {
//                        return new URLStreamHandler() {
//                            @Override
//                            protected URLConnection openConnection(URL url) throws IOException {
//                                return new URLConnection(url) {
//                                    public void connect() throws IOException {
//                                    }
//
////                                    public InputStream getInputStream() throws IOException {
////                                        System.out.println("Someone is getting my jar!!");
////                                        return new ByteArrayInputStream(null);
////                                    }
//                                };
//                            }
//                        };
//                    }
//                    return null;
//                }
//            };
//            TomcatURLStreamHandlerFactory factory = TomcatURLStreamHandlerFactory.getInstance();
//            TomcatURLStreamHandlerFactory.register();
//            factory.addUserFactory(factory1);
//
//            URL.setURLStreamHandlerFactory(factory);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /*
     * Required when this classloader is used as the system classloader
     */
    public DynamicClassLoader(ClassLoader parent) {
        this("classpath", parent);
    }

    public DynamicClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /* this is why needed to extend URLClassLoader, so get dynamic adding */
    public void add(URL url) {
        addURL(url);
    }

    // THIS APPROACH hacks the URL handler to return a bytearray input stream if a particular custom protocol is detected
    //    Works except can't keep setting URL.setURLStreamHandlerFactory(), not allowed
    //    And I need to keep changing the bytearayinputstream it returns
    public void addBytecode(String cidHash, byte[] data) throws Exception {
//        URLStreamHandlerFactory factory = new URLStreamHandlerFactory() {
//            public URLStreamHandler createURLStreamHandler(String urlProtocol) {
//                System.out.println("Someone asked for protocol: " + urlProtocol);
//                if ("myjarprotocol".equalsIgnoreCase(urlProtocol)) {
//                    return new URLStreamHandler() {
//                        @Override
//                        protected URLConnection openConnection(URL url) throws IOException {
//                            return new URLConnection(url) {
//                                public void connect() throws IOException {}
//                                public InputStream getInputStream() throws IOException {
//                                    System.out.println("Someone is getting my jar!!");
//                                    return new ByteArrayInputStream(data);
//                                }
//                            };
//                        }
//                    };
//                }
//                return null;
//            }
//        };
//
//        //URL.setURLStreamHandlerFactory(null);
//        URL.setURLStreamHandlerFactory(factory);
        //forcefullyInstall(factory);



//        URLStreamHandler handler = new URLStreamHandler() {
//            @Override
//            protected URLConnection openConnection(URL url) throws IOException {
//                return new URLConnection(url) {
//                    public void connect() throws IOException {}
//                    public InputStream getInputStream() throws IOException {
//                        System.out.println("Someone is getting my jar!!");
//                        return new ByteArrayInputStream(data);
//                    }
//                };
//            }
//        };

        //Trigger jar detection via URL streamhandler override
        //add(new URL(null, "arb", handler));

        //setData(data);

//        factory = new URLStreamHandlerFactory() {
//            public URLStreamHandler createURLStreamHandler(String urlProtocol) {
//                System.out.println("Someone asked for protocol: " + urlProtocol);
//                if ("myjarprotocol".equalsIgnoreCase(urlProtocol)) {
//                    return new URLStreamHandler() {
//                        @Override
//                        protected URLConnection openConnection(URL url) throws IOException {
//                            return new URLConnection(url) {
//                                public void connect() throws IOException {
//                                }
//
//                                public InputStream getInputStream() throws IOException {
//                                    System.out.println("Someone is getting my jar!!");
//                                    return new ByteArrayInputStream(data);
//                                }
//                            };
//                        }
//                    };
//                }
//                return null;
//            }
//        };

        // NEED TO SOMEHOW add the data[] to the URLStreamHandlerProvider

        //entries.put(cidHash, data);


        try {
            URLStreamHandlerFactory factory1 = new URLStreamHandlerFactory() {
                public URLStreamHandler createURLStreamHandler(String urlProtocol) {
                    //System.out.println("Someone asked for protocol: " + urlProtocol);
                    if ("myjarprotocol".equalsIgnoreCase(urlProtocol)) {
                        return new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL url) throws IOException {
                                return new URLConnection(url) {
                                    public void connect() throws IOException {
                                    }
                                    public InputStream getInputStream() throws IOException {
                                        System.out.println("Someone is getting my jar!!, of length: "+data.length);
                                        return new ByteArrayInputStream(data);
                                    }
                                };
                            }
                        };
                    }
                    return null;
                }
            };
            TomcatURLStreamHandlerFactory factory = TomcatURLStreamHandlerFactory.getInstance();
            factory.addUserFactory(factory1);
            TomcatURLStreamHandlerFactory.register();

            //TomcatURLStreamHandlerFactory.register();
            //TomcatURLStreamHandlerFactory.disable();


            //URL.setURLStreamHandlerFactory(factory);
            System.out.println("Factories Size 1: "+factory.getUserFactories().size());

            System.out.println("TRiggering URL stream handler");
            add(new URL("myjarprotocol:arb"));
            //add(new URL(null, "bytes:///" + cidHash, new BytesHandler()));

            //TomcatURLStreamHandlerFactory.release(ClassLoader.getSystemClassLoader());
            //factory.
            System.out.println("Factories Size 2: "+factory.getUserFactories().size());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    // DOESNT WORK: java.lang.reflect.InaccessibleObjectException: Unable to make field private static volatile java.net.URLStreamHandlerFactory java.net.URL.factory accessible: module java.base does not "opens java.net" to unnamed module @7e774085
//    public static void forcefullyInstall(URLStreamHandlerFactory factory) {
//        try {
//            // Try doing it the normal way
//            URL.setURLStreamHandlerFactory(factory);
//        } catch (final Error e) {
//            // Force it via reflection
//            try {
//                final Field factoryField = URL.class.getDeclaredField("factory");
//                factoryField.setAccessible(true);
//                factoryField.set(null, factory);
//            } catch (NoSuchFieldException | IllegalAccessException e1) {
//                throw new Error("Could not access factory field on URL class: {}", e);
//            }
//        }
//    }

    public static DynamicClassLoader findAncestor(ClassLoader cl) {
        do {

            if (cl instanceof DynamicClassLoader)
                return (DynamicClassLoader) cl;

            cl = cl.getParent();
        } while (cl != null);

        return null;
    }

    /*
     *  Required for Java Agents when this classloader is used as the system classloader
     */
    @SuppressWarnings("unused")
    private void appendToClassPathForInstrumentation(String jarfile) throws IOException {
        add(Paths.get(jarfile).toRealPath().toUri().toURL());
    }



    ///// ADDITONAL FROM REMOTECLASSLOADER
    ///// TRYING TO MERGE ABILITY TO LOAD FROM byte[], CANT get it to work

//    public void addJar(byte[] jarBytes) throws IOException {
//        this.jarBytes = jarBytes;
//        this.names = DynamicClassLoader.loadNames(jarBytes);
//    }
//
//    private static Set<String> loadNames(byte[] jarBytes) throws IOException {
//        Set<String> set = new HashSet<>();
//        try (ZipInputStream jis =
//                     new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
//            ZipEntry entry;
//            while ((entry = jis.getNextEntry()) != null) {
//                set.add(entry.getName());
//            }
//        }
//        return Collections.unmodifiableSet(set);
//    }
//
//    @Override
//    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        //System.out.println("Loading class: "+name);
//        Class<?> clazz = findLoadedClass(name);
//        if (clazz == null) {
//            //System.out.println("Loading class from custom set: "+name);
//            try {
//                InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                StreamUtils.copy(in, out);
//                byte[] bytes = out.toByteArray();
//                clazz = defineClass(name, bytes, 0, bytes.length);
//                if (resolve) {
//                    resolveClass(clazz);
//                }
//            } catch (Exception e) {
//                clazz = super.loadClass(name, resolve);
//            }
//        }
//        return clazz;
//    }
//
//    @Override
//    public URL getResource(String name) {
//        //System.out.println("Getting resource: "+name);
//        return null;
//    }
//
//    @Override
//    public InputStream getResourceAsStream(String name) {
//        //System.out.println("Getting resource stream: "+name);
//        // Check first if the entry name is known
//        if (!names.contains(name)) {
//            //System.out.println("Not in names: "+name);
//            return null;
//        }
//        // I moved the JarInputStream declaration outside the
//        // try-with-resources statement as it must not be closed otherwise
//        // the returned InputStream won't be readable as already closed
//        boolean found = false;
//        ZipInputStream jis = null;
//        try {
//            jis = new ZipInputStream(new ByteArrayInputStream(jarBytes));
//            ZipEntry entry;
//            while ((entry = jis.getNextEntry()) != null) {
//                if (entry.getName().equals(name)) {
//                    found = true;
//                    return jis;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // Only close the stream if the entry could not be found
//            if (jis != null && !found) {
//                try {
//                    jis.close();
//                } catch (IOException e) {
//                    // ignore me
//                }
//            }
//        }
//        return null;
//    }

    //ADDITIONAL FROM: https://stackoverflow.com/questions/17776884/any-way-to-create-a-url-from-a-byte-array
//    public URL getResource(String name) {
//        System.out.println("Getting resource: "+name);
//        try {
//            return new URL(null, "bytes:///" + name, new BytesHandler());
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

//    class BytesHandler extends URLStreamHandler {
//
//        public BytesHandler() {
//            System.out.println("Making bytes handler");
//        }
//
//        @Override
//        protected URLConnection openConnection(URL u) throws IOException {
//            System.out.println("Opening connection: "+u.toString());
//            return new ByteUrlConnection(u);
//        }
//    }
//
//    class ByteUrlConnection extends URLConnection {
//        public ByteUrlConnection(URL url) {
//            super(url);
//            System.out.println("Making byte url connection: "+url.toString());
//        }
//
//        @Override
//        public void connect() throws IOException {
//        }
//
//        @Override
//        public InputStream getInputStream() throws IOException {
//            return new ByteArrayInputStream(entries.get(this.getURL().getPath().substring(1)));
//        }
//    }

}