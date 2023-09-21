package tech.edgx.drf.util_alternate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

public final class SomeClassLoader extends ClassLoader {
    private final Map<String, byte[]> entries = new HashMap<>();

    public URL getResource(String name) {

        try {
            return new URL(null, "bytes:///" + name, new BytesHandler());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    class BytesHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new ByteUrlConnection(u);
        }
    }

    class ByteUrlConnection extends URLConnection {
        public ByteUrlConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(entries.get(this.getURL().getPath().substring(1)));
        }
    }
}