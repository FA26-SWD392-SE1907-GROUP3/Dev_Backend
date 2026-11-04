package com.example.swd392_se1907_aives.modules.examination.internal.service;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GradingModelTests {
    @Test void solGradingUsesMediumReasoningAndPreservesTheStrictRubricOutputContract()throws Exception{
        var json=new ObjectMapper();var sent=new AtomicReference<JsonNode>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        String grade="{\"feedback\":\"Đủ ý\",\"criteria\":[{\"sortOrder\":1,\"score\":8,\"feedback\":\"Lập luận tốt\"}]}";
        byte[] response=json.writeValueAsBytes(Map.of("status","completed","output",List.of(Map.of("content",List.of(Map.of("type","output_text","text",grade))))));
        server.createContext("/responses",exchange->{sent.set(json.readTree(exchange.getRequestBody()));exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();});server.start();
        try{
            var client=new AttemptGradingAiClient(json,"fake","gpt-6.1-sol","http://127.0.0.1:"+server.getAddress().getPort());
            assertThat(client.grade("private evidence").at("/criteria/0/score").asInt()).isEqualTo(8);
            assertThat(sent.get().path("model").asText()).isEqualTo("gpt-6.1-sol");
            assertThat(sent.get().at("/reasoning/effort").asText()).isEqualTo("medium");
            assertThat(sent.get().at("/text/format/strict").asBoolean()).isTrue();
            assertThat(sent.get().path("store").asBoolean()).isFalse();
        }finally{server.stop(0);}
    }
}
