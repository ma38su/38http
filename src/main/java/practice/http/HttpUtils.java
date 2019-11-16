package practice.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpUtils {

    private static final Pattern HEADER_KEYVALUE_PATTERN = Pattern.compile("(?<key>\\S+)\\:\\s*(?<value>\\S.*)");

    public static Map<String, String> parseHeaders(InputStream in) throws IOException {
        var headers = new HashMap<String, String>();
        while (true) {
            String line = readLine(in);
            if ("".equals(line)) {
                break;
            }
            var matcher = HEADER_KEYVALUE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("Illegal header: "+ line);
            }
            String key = matcher.group("key");
            String value = matcher.group("value");
            System.out.println("header: "+ key + ": "+ value);
            headers.put(key, value);
        }
        return headers;
    }
    
    public static Map<String, String> parseHeaders(BufferedReader in) throws IOException {
        var headers = new HashMap<String, String>();

        String line;
        while ((line = in.readLine()) != null) {
            if ("".equals(line)) {
                break;
            }
            var matcher = HEADER_KEYVALUE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("Illegal header: "+ line);
            }
            String key = matcher.group("key");
            String value = matcher.group("value");
            System.out.println("header: "+ key + ": "+ value);
            headers.put(key, value);
        }
        return headers;
    }
    public static int getContentLength(Map<String, String> headers) {
        String value = headers.get("Content-Length");
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }
    
    public static String readLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        readLine(is, sb);
        return sb.toString();
    }

    public static int readLine(InputStream is, StringBuilder sb) throws IOException {
        int size = 0;
        int i;
        while ((i = is.read()) != -1) {
            char c = (char) i;
            size += 1;
            if (c == '\n') {
                break;
            }
            if (c == '\r') {
                continue;
            }
            sb.append(c);
        }
        return size;
    }

    public static String read(InputStream in, byte[] buf, int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        
        int l = buf.length;
        while (size > 0) {
            if (l > size) {
                l = size;
            }
            int i = in.read(buf, 0, l);
            size -= i;
            out.write(buf, 0, i);
        }
        return out.toString();
    }

    public static String read(InputStream in, int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        for (int i = 0; i < size; ++i) {
            char c = (char) in.read();
            out.write(c);
        }
        return out.toString();
    }
    
    public static void skipCRLF(InputStream in) throws IOException {
        if (in.read() != '\r' || in.read() != '\n') {
            throw new IllegalStateException();
        }
    }


}
