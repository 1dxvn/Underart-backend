package com.underart.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class R2MediaStorageAdapterTest {

    @Test
    void uploadsAnImageAndReadsItsHeaderThroughTheRealSdkTransport() throws Exception {
        byte[] image = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01};
        var uploaded = new AtomicReference<byte[]>();
        var range = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/artwork-bucket/image.png", exchange -> {
            try (exchange) {
                if ("PUT".equals(exchange.getRequestMethod())) {
                    uploaded.set(exchange.getRequestBody().readAllBytes());
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                if ("GET".equals(exchange.getRequestMethod())) {
                    range.set(exchange.getRequestHeaders().getFirst("Range"));
                    byte[] header = Arrays.copyOf(uploaded.get(), 8);
                    exchange.getResponseHeaders().set("Content-Range", "bytes 0-7/" + image.length);
                    exchange.sendResponseHeaders(206, header.length);
                    exchange.getResponseBody().write(header);
                    return;
                }
                exchange.sendResponseHeaders(405, -1);
            }
        });
        server.start();
        var storage = new R2MediaStorageAdapter("", "test-key", "test-secret", "artwork-bucket",
                "https://cdn.example.test", "http://127.0.0.1:" + server.getAddress().getPort(), "us-east-2");
        try {
            storage.upload("image.png", new ByteArrayInputStream(image), image.length, "image/png");
            assertThat(uploaded.get()).containsExactly(image);
            assertThat(storage.readPrefix("image.png", 8)).containsExactly(Arrays.copyOf(image, 8));
            assertThat(range.get()).isEqualTo("bytes=0-7");
        } finally {
            storage.close();
            server.stop(0);
        }
    }
}
