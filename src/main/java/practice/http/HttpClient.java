package practice.http;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class HttpClient implements Closeable{

    public static void main(String[] args) throws IOException {
        try (HttpClient client = new HttpClient()) {
            client.open("www.google.co.jp", 80);
            client.requestGet2();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
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
    
    public void requestGet2() {
        String method = "GET";
        String uri = "/";
        String httpVersion = "HTTP/2";

        String requestLine = String.format("%s %s %s", method, uri, httpVersion);
        var headers = Arrays.asList(requestLine);
        this.request(headers, null);
    }

    public void request(List<String> reqHeaders, String reqBody) {
        try (var out = new PrintStream(this.socket.getOutputStream())) {
            for (var header : reqHeaders) {
                out.println(header);
            }
            out.println("");
            if (reqBody != null) {
                out.println(reqBody);
            }

            this.parseResponse();
        } catch (IOException e) {
            System.out.println(socket.getRemoteSocketAddress() + ": "+ e.getMessage());
        }
    }
    
    private static final Pattern STATUS_LINE_PATTERN = Pattern.compile("(?<version>\\S+)\\s+(?<state>\\d+)\\s+(?<phrase>\\S+)");

    private void parseResponse() throws IOException {
        try (var in = new BufferedInputStream(this.socket.getInputStream())) {
            String startLine = HttpUtils.readLine(in);
            var matcher = STATUS_LINE_PATTERN.matcher(startLine);
            if (!matcher.matches()) {
                throw new IllegalStateException("Illegal status line: "+ startLine);
            }
            
            String httpVersion = matcher.group("version");
            String state = matcher.group("state");
            String phrase = matcher.group("phrase");

            System.out.println("http-version: "+ httpVersion);
            System.out.println("status-code: "+ state);
            System.out.println("reason-phrase: "+ phrase);

            var resHeaders = HttpUtils.parseHeaders(in);

            byte[] buf = new byte[2048];

            int contentLength = HttpUtils.getContentLength(resHeaders);
            if (contentLength >= 0) {
                String b = HttpUtils.read(in, buf, contentLength);
                System.out.println(b);
                return;
            }

            String transferEncoding = resHeaders.get("Transfer-Encoding");
            if ("chunked".equals(transferEncoding.toLowerCase())) {
                try {
                    while (true) {
                        String hex = HttpUtils.readLine(in);
                        int chunkSize = Integer.parseInt(hex, 16);
                        if (chunkSize == 0) {
                            break;
                        }
                        
                        String body = HttpUtils.read(in, buf, chunkSize);
                        HttpUtils.skipCRLF(in);
                        System.out.print(body);
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
                return;
            }
            throw new IllegalStateException();
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
