package com.example.swd392_se1907_aives.modules.examination.internal.config;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionTickets;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewDiagnostics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class LiveTranscriptionHandler extends AbstractWebSocketHandler {
    private final LiveTranscriptionTickets tickets;
    private final InterviewAiClient ai;
    private final ObjectMapper json;
    private final InterviewStateStore store;
    private final Map<String,Connection> connections=new ConcurrentHashMap<>();
    private final Map<UUID,String> activePrompts=new ConcurrentHashMap<>();
    public LiveTranscriptionHandler(LiveTranscriptionTickets tickets,InterviewAiClient ai,ObjectMapper json,InterviewStateStore store){this.tickets=tickets;this.ai=ai;this.json=json;this.store=store;}
    @Override public void afterConnectionEstablished(WebSocketSession raw)throws Exception{
        var session=new ConcurrentWebSocketSessionDecorator(raw,10000,128000);
        try {
            String token=UriComponentsBuilder.fromUri(raw.getUri()).build().getQueryParams().getFirst("ticket");
            var ticket=tickets.consume(token);
            // A fresh one-use ticket replaces this owner's stale socket during reconnect/replay.
            String previousId=activePrompts.put(ticket.promptId(),raw.getId());
            if(previousId!=null){var previous=connections.get(previousId);if(previous!=null)previous.close();}
            raw.setBinaryMessageSizeLimit(48000);var connection=new Connection(session,ticket);connections.put(raw.getId(),connection);
            ai.connectRealtime(connection).whenComplete((socket,error)->{if(error!=null)connection.fail();});
        }catch(Exception e){session.close(CloseStatus.POLICY_VIOLATION);}
    }
    @Override protected void handleBinaryMessage(WebSocketSession session,BinaryMessage message){
        var connection=connections.get(session.getId());if(connection==null)return;
        if(!connection.ticket.deadline().isAfter(Instant.now())){connection.close();return;}
        byte[] bytes=new byte[message.getPayloadLength()];message.getPayload().get(bytes);connection.received+=bytes.length;
        long allowed=(Duration.between(connection.opened,Instant.now()).toMillis()+2000)*48;
        if(bytes.length%2!=0 || connection.received>allowed || connection.received>29_000_000){connection.fail();return;}
        connection.send(Map.of("type","input_audio_buffer.append","audio",Base64.getEncoder().encodeToString(bytes)));
    }
    @Override protected void handleTextMessage(WebSocketSession session,TextMessage message){
        var connection=connections.get(session.getId());if(connection==null)return;
        try{if(message.getPayloadLength()<1000 && "commit".equals(json.readTree(message.getPayload()).path("type").asText())){
            connection.commitAt=System.nanoTime();connection.trace.event("commit","audioBytes",connection.received);
            connection.send(Map.of("type","input_audio_buffer.commit"));}}
        catch(Exception e){connection.fail();}
    }
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){var connection=connections.remove(session.getId());if(connection!=null)connection.close();}
    @Override public void handleTransportError(WebSocketSession session,Throwable error){var connection=connections.get(session.getId());if(connection!=null)connection.close();}
    @Scheduled(fixedDelay=1000) public void expire(){for(var connection:connections.values())if(!connection.ticket.deadline().isAfter(Instant.now())||!store.voiceTurnOpen(connection.ticket.promptId())){
        connection.trace.event("server_expire","status","deadline_or_prompt_closed");connection.close();}}
    private final class Connection implements WebSocket.Listener {
        final WebSocketSession browser;final LiveTranscriptionTickets.Ticket ticket;final Instant opened=Instant.now();
        final StringBuilder buffer=new StringBuilder();volatile WebSocket upstream;volatile boolean closed;long received;
        CompletableFuture<Void> pending=CompletableFuture.completedFuture(null);
        final InterviewDiagnostics.Span trace;long commitAt;boolean firstDelta=true;
        Connection(WebSocketSession browser,LiveTranscriptionTickets.Ticket ticket){this.browser=browser;this.ticket=ticket;
            trace=InterviewDiagnostics.start("stt.live","promptId",ticket.promptId(),"source","live","model",ai.realtimeModel(),"sampleRate",24000,"format","pcm16");}
        @Override public void onOpen(WebSocket socket){
            upstream=socket;if(closed){socket.abort();return;}socket.sendText(ai.realtimeConfiguration(),true).whenComplete((v,e)->{if(e!=null)fail();});socket.request(1);
        }
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){
            buffer.append(data);
            if(buffer.length()>200000){fail();return CompletableFuture.completedFuture(null);}
            if(last){
                try {
                    var event=json.readTree(buffer.toString());String type=event.path("type").asText();
                    if(Set.of("session.updated","transcription_session.updated").contains(type)){trace.event("ready");emit(Map.of("type","ready"));}
                    else if("conversation.item.input_audio_transcription.delta".equals(type)){
                        if(firstDelta){firstDelta=false;trace.event("first_delta","firstTokenMs",trace.elapsedMs());}
                        emit(Map.of("type","delta","itemId",event.path("item_id").asText(),"text",event.path("delta").asText()));}
                    else if("conversation.item.input_audio_transcription.completed".equals(type)){
                        trace.event("final","transcriptChars",event.path("transcript").asText().length(),"sinceCommitMs",commitAt==0?-1:TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-commitAt));
                        emit(Map.of("type","final","itemId",event.path("item_id").asText(),"text",event.path("transcript").asText()));}
                    else if("error".equals(type) || "conversation.item.input_audio_transcription.failed".equals(type))fail();
                }catch(Exception e){fail();}finally{buffer.setLength(0);}
            }
            socket.request(1);return CompletableFuture.completedFuture(null);
        }
        @Override public CompletionStage<?> onClose(WebSocket socket,int code,String reason){close();return CompletableFuture.completedFuture(null);}
        @Override public void onError(WebSocket socket,Throwable error){fail();}
        synchronized void send(Map<String,Object> event){
            if(upstream==null || closed)return;
            try{String message=json.writeValueAsString(event);pending=pending.thenCompose(v->upstream.sendText(message,true).thenApply(ws->(Void)null));pending.whenComplete((v,e)->{if(e!=null)fail();});}
            catch(Exception e){fail();}
        }
        void emit(Object event){try{if(browser.isOpen())browser.sendMessage(new TextMessage(json.writeValueAsString(event)));}catch(Exception e){close();}}
        void fail(){emit(Map.of("type","error","message","Nhận dạng trực tiếp bị gián đoạn. Bản ghi vẫn được giữ để nhận dạng khi nộp."));close();}
        void close(){
            if(!closed)trace.event("closed","audioBytes",received);
            if(closed)return;closed=true;connections.remove(browser.getId(),this);activePrompts.remove(ticket.promptId(),browser.getId());
            if(upstream!=null)upstream.abort();try{if(browser.isOpen())browser.close();}catch(Exception ignored){}
        }
    }
}
