package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionTickets;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:live-ws;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "JWT_SECRET=live_websocket_test_secret_at_least_32_characters_long"})
class LiveWebSocketIntegrationTests {
    @LocalServerPort int port;
    @Autowired LiveTranscriptionTickets tickets;
    @MockitoBean InterviewAiClient ai;
    final HttpClient http=HttpClient.newHttpClient();

    URI uri(String ticket){return URI.create("ws://localhost:"+port+"/api/v1/interviews/live?ticket="+ticket);}
    static class Listener implements WebSocket.Listener {
        final CompletableFuture<Integer> closed=new CompletableFuture<>();
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        @Override public void onOpen(WebSocket socket){socket.request(1);}
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){messages.add(data.toString());socket.request(1);return CompletableFuture.completedFuture(null);}
        @Override public CompletionStage<?> onClose(WebSocket socket,int code,String reason){closed.complete(code);return CompletableFuture.completedFuture(null);}
    }
    @Test void invalidTicketStillReachesWebSocketHandlerRatherThanMvcUuidConversion()throws Exception{
        for(String origin:new String[]{"http://localhost:3000","http://127.0.0.1:3000"}){
            var listener=new Listener();
            var socket=http.newWebSocketBuilder().header("Origin",origin).buildAsync(uri("invalid"),listener).get(5,TimeUnit.SECONDS);
            assertThat(listener.closed.get(5,TimeUnit.SECONDS)).isEqualTo(1008);
            socket.abort();
        }
    }
    @Test void validOneUseTicketRelaysProviderReadyAndCaptionThroughRealEmbeddedServer()throws Exception{
        var upstream=mock(WebSocket.class);
        when(upstream.sendText(anyString(),eq(true))).thenReturn(CompletableFuture.completedFuture(upstream));
        when(ai.realtimeConfiguration()).thenReturn("{\"type\":\"session.update\"}");
        when(ai.connectRealtime(any())).thenAnswer(invocation->{
            WebSocket.Listener listener=invocation.getArgument(0);
            listener.onOpen(upstream);
            listener.onText(upstream,"{\"type\":\"session.updated\"}",true);
            listener.onText(upstream,"{\"type\":\"conversation.item.input_audio_transcription.delta\",\"item_id\":\"test-caption\",\"delta\":\"Test caption\"}",true);
            return CompletableFuture.completedFuture(upstream);
        });
        String ticket=tickets.issue(new InterviewStateStore.LivePermission(UUID.randomUUID(),UUID.randomUUID(),OffsetDateTime.now().plusMinutes(1)));
        var listener=new Listener();
        var socket=http.newWebSocketBuilder().header("Origin","http://localhost:3000").buildAsync(uri(ticket),listener).get(5,TimeUnit.SECONDS);
        assertThat(listener.messages.poll(5,TimeUnit.SECONDS)).contains("ready");
        assertThat(listener.messages.poll(5,TimeUnit.SECONDS)).contains("delta","Test caption");
        socket.sendClose(WebSocket.NORMAL_CLOSURE,"done").get(5,TimeUnit.SECONDS);
        var replay=new Listener();
        var second=http.newWebSocketBuilder().header("Origin","http://localhost:3000").buildAsync(uri(ticket),replay).get(5,TimeUnit.SECONDS);
        assertThat(replay.closed.get(5,TimeUnit.SECONDS)).isEqualTo(1008);second.abort();
    }
    @Test void untrustedOriginIsRejectedBeforeUpgradeAndRestStillRequiresJwt()throws Exception{
        try {
            http.newWebSocketBuilder().header("Origin","https://untrusted.example").buildAsync(uri("invalid"),new Listener()).get(5,TimeUnit.SECONDS);
            fail("Untrusted origin must not upgrade");
        }catch(ExecutionException error){
            assertThat(error.getCause()).isInstanceOf(WebSocketHandshakeException.class);
            assertThat(((WebSocketHandshakeException)error.getCause()).getResponse().statusCode()).isEqualTo(403);
        }
        var response=http.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/interviews/"+UUID.randomUUID())).GET().build(),HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
