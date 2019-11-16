package practice.http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HttpServer implements Closeable {

    public static void main(String[] args) throws IOException {
        try (HttpServer server = new HttpServer()) {
            server.open(8080);
        }
    }

    private ServerSocket serverSocket;
    private Executor executor = Executors.newCachedThreadPool();

    private static final Pattern HEADER_KEYVALUE_PATTERN = Pattern.compile("(?<key>\\S+)\\:\\s*(?<value>\\S.*)");
    private static final Pattern REQUEST_PATTERN = Pattern.compile("(?<method>\\S+)\\s+(?<uri>\\S+)\\s+(?<version>\\S+)");

    public void open(int port) throws UnknownHostException, IOException {

        this.serverSocket = new ServerSocket(port);
        System.out.println("open: "+ this.serverSocket.getLocalPort());

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("request from: "+ socket.getRemoteSocketAddress());
            executor.execute(() -> process(socket));
        }
    }

    private static void process(Socket socket) {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line = in.readLine();
            if (line == null) {
                return;
            }

            var matcher = REQUEST_PATTERN.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("Illegal start-line: "+ line);
            }

            String method = matcher.group("method").toLowerCase();
            String requestUri = matcher.group("uri");
            String httpVersion = matcher.group("version");
            
            var headers = parseHeaders(in);

            System.out.println("from: "+ socket.getRemoteSocketAddress() + ": "+ line);
            System.out.println("method: "+ method);
            System.out.println("request-uri: "+ requestUri);
            System.out.println("http-version: "+ httpVersion);

            switch (method) {
                case "get": {
                    processForGet(in, requestUri);
                    break;                    
                }
                case "post": {
                    processForPost(in, headers);
                    break;                    
                }
            }

            try (var out = new PrintStream(socket.getOutputStream())) {
                out.println(String.format("%s 200 OK", httpVersion));
                out.println("Content-Type: text/html; charset=Shift_JIS");
                out.println();
                
                transferFile(out, "/form.html");
                System.out.println("to: "+ socket.getRemoteSocketAddress());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(socket.getRemoteSocketAddress() + ": "+ e.getClass().getSimpleName() + " - "+ e.getMessage());
        }
    }

    private static Map<String, String> parseHeaders(BufferedReader in) throws IOException {
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
    
    private static void processForPost(BufferedReader in, Map<String, String> headers) throws IOException {
        int contentLength = Integer.parseInt(headers.get("Content-Length"));
        String contentType = headers.get("Content-Type");
        String transferEncoding = headers.get("Transfer-Encoding");
        System.out.println("content-type: "+ contentType);
        System.out.println("transfer-encoding: "+ transferEncoding);
        System.out.println("post begin");
        char[] buf = new char[contentLength];
        in.read(buf, 0, contentLength);
        System.out.println(new String(buf));
        System.out.println("post end");
    }

    private static void processForGet(BufferedReader in, String requestUri) throws IOException {
        int index = requestUri.indexOf('?');
        String uri;
        if (index >= 0) {
            uri = requestUri.substring(0, index);
            System.out.println("uri: "+ uri);
            String getQuery = requestUri.substring(index + 1);
            System.out.println("get-query: "+ getQuery);
        } else {
            uri = requestUri;
        }
    }

    private static void transferFile(PrintStream out, String resoucePath) throws IOException {
        try (var fileStream = HttpServer.class.getResourceAsStream(resoucePath)) {
            byte[] buf = new byte[2048];
            int read;
            while ((read = fileStream.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        ServerSocket socket = this.serverSocket;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
