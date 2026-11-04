package com.example.swd392_se1907_aives.modules.examination.internal.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class InterviewTranscriptionClientTests {
    @ParameterizedTest @ValueSource(strings={"gpt-transcribe","gpt-4o-mini-transcribe"})
    void sendsTheLanguageFieldSupportedByTheSelectedModel(String model)throws Exception{
        var body=new AtomicReference<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/audio/transcriptions",exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            var response="{\"text\":\"Em chọn cách tách trách nhiệm.\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try{
            var client=new InterviewAiClient(new ObjectMapper(),"fake","http://127.0.0.1:"+server.getAddress().getPort(),
                    "gpt-6.1-sol",model,"gpt-realtime-2.1-mini","marin","gpt-live-transcribe","ws://localhost");
            assertThat(client.transcribe(new byte[]{1,2},"audio/wav","wav")).contains("tách trách nhiệm");
            if(model.equals("gpt-transcribe"))assertThat(body.get()).contains("name=\"languages[]\"").doesNotContain("name=\"language\"");
            else assertThat(body.get()).contains("name=\"language\"").doesNotContain("name=\"languages[]\"");
        }finally{server.stop(0);}
    }
}
