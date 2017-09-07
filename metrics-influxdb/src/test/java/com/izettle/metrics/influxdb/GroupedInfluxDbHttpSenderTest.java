package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class GroupedInfluxDbHttpSenderTest {

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    @Test
    public void shouldNotThrowException() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(10081), 0);
        try {
            server.createContext("/write", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            InfluxDbHttpSender influxDbHttpSender = new GroupedInfluxDbHttpSender(
                "http",
                "localhost",
                10081,
                "testdb",
                "asdf",
                TimeUnit.MINUTES,
                1000,
                1000,
                "",
                "MyGroup"
            );
            assertThat(influxDbHttpSender.writeData(new byte[0]) == 0);
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            server.stop(0);
        }
    }
}
