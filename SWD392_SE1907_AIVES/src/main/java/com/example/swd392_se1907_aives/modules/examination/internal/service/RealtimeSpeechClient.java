package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Realtime is only a reader. Sol and the state store remain responsible for exam decisions. */
public final class RealtimeSpeechClient {
    private final ObjectMapper json;
    private final String key, model, voice, instructions;
    private final URI endpoint;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Semaphore slots = new Semaphore(4);

    public RealtimeSpeechClient(ObjectMapper json, String key, String model, String voice, String baseUrl) {
        this.json=json; this.key=key; this.model=model; this.voice=voice;
        this.endpoint=URI.create(baseUrl.replaceFirst("^http", "ws").replaceAll("/+$", "")
                + "/realtime?model=" + java.net.URLEncoder.encode(model, StandardCharsets.UTF_8));
        try { instructions=new ClassPathResource("prompts/interview-voice.txt").getContentAsString(StandardCharsets.UTF_8); }
        catch(java.io.IOException e) { throw new IllegalStateException("Missing voice prompt",e); }
    }

    public byte[] speak(String text) {
        if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Giọng đọc đang bận. Hãy thử lại.");
        Reader listener=new Reader(text);
        CompletableFuture<WebSocket> connection=null;
        try {
            connection=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                    .header("Authorization","Bearer "+key).buildAsync(endpoint,listener);
            connection.get(12,TimeUnit.SECONDS);
            return listener.result.get(45,TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            listener.trace.event("error","errorType",e.getClass().getSimpleName());
            Thread.currentThread().interrupt(); throw failure();
        } catch(Exception e) { listener.trace.event("error","errorType",e.getClass().getSimpleName());throw failure(); }
        finally { listener.close(); if(connection!=null)connection.whenComplete((s,e)->{if(s!=null)s.abort();}); slots.release(); }
    }

    private ResponseStatusException failure(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Không tạo được giọng đọc AI. Có thể dùng giọng đọc dự phòng.");}

    final class Reader implements WebSocket.Listener {
        final CompletableFuture<byte[]> result=new CompletableFuture<>();
        final ByteArrayOutputStream audio=new ByteArrayOutputStream();
        final StringBuilder message=new StringBuilder();
        final String text;
        final InterviewDiagnostics.Span trace;
        boolean firstAudio=true;
        WebSocket socket;
        boolean requested,closed;
        Reader(String text){this.text=text;trace=InterviewDiagnostics.start("tts.realtime","source","live","model",model,
                "voiceId",voice,"speed",1,"pitch","provider_default","sampleRate",24000,"format","pcm16_wav","textChars",text.length());}
        @Override public void onOpen(WebSocket ws){
            socket=ws;if(closed){ws.abort();return;}
            send(Map.of("type","session.update","session",Map.of("type","realtime","model",model,
                    "output_modalities",List.of("audio"),"audio",Map.of("output",Map.of(
                            "format",Map.of("type","audio/pcm","rate",24000),"voice",voice)))));
            ws.request(1);
        }
        void send(Object value){try{socket.sendText(json.writeValueAsString(value),true).whenComplete((v,e)->{if(e!=null)result.completeExceptionally(e);});}
            catch(Exception e){result.completeExceptionally(e);}}
        @Override public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last){
            if(closed)return CompletableFuture.completedFuture(null);
            message.append(data);
            try {
                if(message.length()>1_000_000)throw new IllegalStateException("Oversized speech event");
                if(last){var event=json.readTree(message.toString());message.setLength(0);
                    String type=event.path("type").asText();
                    if(type.equals("session.updated")&&!requested){
                        requested=true;
                        send(Map.of("type","response.create","response",Map.of("conversation","none",
                                "output_modalities",List.of("audio"),"instructions",instructions,"max_output_tokens",4096,
                                "input",List.of(Map.of("type","message","role","user","content",List.of(
                                        Map.of("type","input_text","text",text)))))));
                    } else if(type.equals("response.output_audio.delta")) {
                        if(firstAudio){firstAudio=false;trace.event("first_byte","firstByteMs",trace.elapsedMs());}
                        byte[] bytes=Base64.getDecoder().decode(event.path("delta").asText());
                        if(audio.size()+bytes.length>8*1024*1024)throw new IllegalStateException("Speech too long");
                        audio.write(bytes);
                    } else if(type.equals("response.done")){
                        trace.event("terminal","status",event.path("response").path("status").asText(),
                                "finishReason",event.path("response").path("status_details").path("reason").asText(event.path("response").path("status").asText("unknown")),"audioBytes",audio.size());
                        if(!"completed".equals(event.path("response").path("status").asText())||audio.size()==0||audio.size()%2!=0)
                            throw new IllegalStateException("Incomplete speech");
                        result.complete(wav(audio.toByteArray()));
                    } else if(type.equals("error")) {trace.event("provider_error","status","failed");throw new IllegalStateException("Speech provider failed");}
                }
            } catch(Exception e){trace.event("error","errorType",e.getClass().getSimpleName());result.completeExceptionally(e);close();}
            if(!closed)ws.request(1);return CompletableFuture.completedFuture(null);
        }
        @Override public void onError(WebSocket ws,Throwable e){trace.event("error","errorType",e.getClass().getSimpleName());result.completeExceptionally(e);}
        @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason){result.completeExceptionally(new IllegalStateException("Speech stream closed"));return CompletableFuture.completedFuture(null);}
        void close(){closed=true;if(socket!=null)socket.abort();}
    }

    static byte[] wav(byte[] samples){
        var out=ByteBuffer.allocate(44+samples.length).order(ByteOrder.LITTLE_ENDIAN);
        out.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36+samples.length).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        out.putInt(16).putShort((short)1).putShort((short)1).putInt(24000).putInt(48000).putShort((short)2).putShort((short)16);
        out.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples.length).put(samples);return out.array();
    }
}
