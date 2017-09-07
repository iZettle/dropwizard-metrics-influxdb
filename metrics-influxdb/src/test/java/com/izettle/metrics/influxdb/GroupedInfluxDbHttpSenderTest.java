package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
        HttpServer server = HttpServer.create(new InetSocketAddress(10085), 0);
        try {
            server.createContext("/write", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            InfluxDbHttpSender influxDbHttpSender = new GroupedInfluxDbHttpSender(
                "http",
                "localhost",
                10085,
                "testdb",
                "asdf",
                TimeUnit.MINUTES,
                1000,
                1000,
                "",
                "MyGroup"
            );
            assertThat(influxDbHttpSender.getSerializer() != null);
            assertThat(influxDbHttpSender.getWriteObject() != null);
            assertThat(influxDbHttpSender.getTags().isEmpty());
            assertThat(influxDbHttpSender.getWriteObject().getDatabase().equals("testdb"));
            assertThat(influxDbHttpSender.writeData(new byte[0]) == 0);
            
            influxDbHttpSender.flush();
            
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            server.stop(0);
        }
    }
}
