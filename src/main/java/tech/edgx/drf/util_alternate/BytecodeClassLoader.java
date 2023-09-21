//package tech.edgx.drf.util;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.jar.JarEntry;
//import java.util.jar.JarInputStream;
//
//public class BytecodeClassLoader extends ClassLoader
//{
//    /*
//     * Default ClassLoader.
//     */
//    private final ClassLoader startup;
//
//    /*
//     * Byte array used to load classes.
//     */
//    private final byte[] bytes;
//
//    /*
//     * HashMap used to contain cached classes.
//     */
//    private HashMap<String, byte[]> classes = new HashMap<>();
//
//    /*
//     * Initializes byte array used for loading classes.
//     * @param ClassLoader classLoader
//     * @param byte[] bytes
//     */
//    public BytecodeClassLoader(ClassLoader classLoader, byte[] bytes)
//    {
//        this.startup = classLoader;
//        this.bytes = bytes;
//    }
//
//    /*
//     * Loads class from name.
//     * (non-Javadoc)
//     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
//     * @param String name
//     * @param boolean resolve
//     * @throws ClassNotFoundException
//     * @returns clazz
//     */
//    @Override
//    public Class<?> loadClass(String name, boolean resolve)
//            throws ClassNotFoundException
//    {
//        Class<?> clazz = findLoadedClass(name);
//        if (clazz == null)
//        {
//            try
//            {
//                InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
//                if (in == null) return null;
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                IOUtils.writeStream(in, out);
//                //StreamUtils.writeTo(in, out);
//                in.close();
//                byte[] bytes = out.toByteArray();
//                out.close();
//                clazz = defineClass(name, bytes, 0, bytes.length);
//                if (resolve)
//                {
//                    resolveClass(clazz);
//                }
//            }
//            catch (Exception e)
//            {
//                clazz = super.loadClass(name, resolve);
//            }
//        }
//        return clazz;
//    }
//
//    /*
//     * Returns resource.
//     * (non-Javadoc)
//     * @see java.lang.ClassLoader#getResource(java.lang.String)
//     * @param String name
//     */
//    @Override
//    public URL getResource(String name)
//    {
//        return null;
//    }
//
//    /*
//     * Returns resource as stream.
//     * (non-Javadoc)
//     * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
//     * @param String name
//     * @return ByteArrayInputStream
//     */
//    @Override
//    public InputStream getResourceAsStream(String name)
//    {
//        InputStream jarRes = this.startup.getResourceAsStream(name);
//        if (jarRes != null)
//        {
//            return jarRes;
//        }
//        if (!this.classes.containsKey(name))
//        {
//            return null;
//        }
//        return new ByteArrayInputStream((byte[])this.classes.get(name));
//    }
//
//    /*
//     * Loads classes using byte array.
//     */
//    public void inject()
//    {
//        if (bytes == null) return;
//        try
//        {
//            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(bytes));
//            try
//            {
//                JarEntry entry;
//                while ((entry = jis.getNextJarEntry()) != null)
//                {
//                    String entryName = entry.getName();
//                    ByteArrayOutputStream out = new ByteArrayOutputStream();
//                    IOUtils.writeStream(jis, out);
//                    byte[] bytes = out.toByteArray();
//                    this.classes.put(entryName, bytes);
//                    this.loadClass(entryName, false);
//                }
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//}