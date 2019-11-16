package practice.http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpClient implements Closeable{

    public static void main(String[] args) throws IOException {
        try (HttpClient client = new HttpClient()) {
            client.open("localhost", 8080);
//            client.requestGet();
            client.requestPost();
        }
    }

    private Socket socket;
    private ExecutorService executor = Executors.newCachedThreadPool();
    
    public HttpClient() {
    }
    
    public void open(String host, int port) throws UnknownHostException, IOException {
        this.socket = new Socket(host, port);
    }
    
    public void requestGet() {
        String method = "GET";
        String uri = "/";
        String httpVersion = "HTTP/1.1";

        String requestLine = String.format("%s %s %s", method, uri, httpVersion);
        var headers = Arrays.asList(requestLine);
        this.request(headers, null);
    }
    
    public void requestPost() {
        String method = "POST";
        String uri = "/";
        String httpVersion = "HTTP/1.1";

        String body = "post-value";
        int contentLength = body.length();

        String requestLine = String.format("%s %s %s", method, uri, httpVersion);
        var headers = Arrays.asList(
                requestLine,
                String.format("Content-Length: %d", contentLength));
        this.request(headers, body);
    }

    public void request(List<String> headers, String body) {
        try (var out = new PrintStream(this.socket.getOutputStream())) {
            for (var header : headers) {
                out.println(header);
            }
            out.println("");
            if (body != null) {
                out.println(body);
            }

            try (var in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()))) {
                String line = null;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println(socket.getRemoteSocketAddress() + ": "+ e.getMessage());
        }
    }
    
    @Override
    public void close() throws IOException {
        Socket socket = this.socket;
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
