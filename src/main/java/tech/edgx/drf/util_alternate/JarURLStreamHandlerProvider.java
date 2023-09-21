//package tech.edgx.drf.util;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.net.URLConnection;
//import java.net.URLStreamHandler;
//import java.net.spi.URLStreamHandlerProvider;
//
// THIS WORKS FOR A ONCE-OFF thing, but I cant dynamically update the data since cant inject as a bean
//public class JarURLStreamHandlerProvider extends URLStreamHandlerProvider {
//
//    public byte[] data = new byte[]{};
//
//    public byte[] getData() {
//        return data;
//    }
//
//    public void setData(byte[] data) {
//        this.data = data;
//    }
//
//    @Override
//    public URLStreamHandler createURLStreamHandler(String protocol) {
//        System.out.println("Streamhandlerprotocol: "+protocol);
//        if ("myjarprotocol".equals(protocol)) {
////            return new URLStreamHandler() {
////                @Override
////                protected URLConnection openConnection(URL u) throws IOException {
////                    return ClassLoader.getSystemClassLoader().getResource(u.getPath()).openConnection();
////                }
////            };
//            return new URLStreamHandler() {
//                @Override
//                protected URLConnection openConnection(URL url) throws IOException {
//                    return new URLConnection(url) {
//                        public void connect() throws IOException {
//                        }
//                        public InputStream getInputStream() throws IOException {
//                            System.out.println("Someone is getting my jar!!");
//                            return new ByteArrayInputStream(data);
//                        }
//                    };
//                }
//            };
//        }
//        return null;
//    }
//
//}