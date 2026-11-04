package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RealtimeSpeechClientTests {
    final ObjectMapper json=new ObjectMapper();
    @Test void usesRealtimeAudioAndReturnsPcmWavOnlyAfterSuccessfulCompletion()throws Exception{
        var client=new RealtimeSpeechClient(json,"fake","gpt-realtime-2.1-mini","marin","http://localhost/v1");
        var reader=client.new Reader("Một câu hỏi cần đọc?");
        var socket=mock(WebSocket.class);var sent=new ArrayList<String>();
        when(socket.sendText(anyString(),eq(true))).thenAnswer(inv->{sent.add(inv.getArgument(0));return CompletableFuture.completedFuture(socket);});
        reader.onOpen(socket);
        var configuration=json.readTree(sent.getFirst());
        assertThat(configuration.at("/session/model").asText()).isEqualTo("gpt-realtime-2.1-mini");
        assertThat(configuration.at("/session/audio/output/voice").asText()).isEqualTo("marin");
        reader.onText(socket,"{\"type\":\"session.updated\"}",true);
        var request=json.readTree(sent.getLast());
        assertThat(request.at("/response/conversation").asText()).isEqualTo("none");
        assertThat(request.at("/response/input/0/content/0/text").asText()).isEqualTo("Một câu hỏi cần đọc?");
        assertThat(request.at("/response/instructions").asText()).contains("NGUYÊN VĂN");
        reader.onText(socket,"{\"type\":\"response.output_audio.delta\",\"delta\":\"AQIDBA==\"}",true);
        assertThat(reader.result.isDone()).isFalse();
        reader.onText(socket,"{\"type\":\"response.done\",\"response\":{\"status\":\"completed\"}}",true);
        byte[] wave=reader.result.get(1,TimeUnit.SECONDS);
        assertThat(new String(wave,0,4,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(wave).hasSize(48);assertThat(Arrays.copyOfRange(wave,44,48)).containsExactly(1,2,3,4);
        reader.close();verify(socket).abort();
    }
    @Test void malformedEmptyOrFailedResponsesNeverProducePlayablePartialAudio(){
        for(String event:List.of("{bad}","{\"type\":\"error\"}","{\"type\":\"response.done\",\"response\":{\"status\":\"completed\"}}",
                "{\"type\":\"response.done\",\"response\":{\"status\":\"failed\"}}")){
            var reader=new RealtimeSpeechClient(json,"fake","gpt-realtime-2.1-mini","marin","http://localhost").new Reader("Text");
            reader.onText(mock(WebSocket.class),event,true);assertThat(reader.result.isCompletedExceptionally()).isTrue();
        }
    }
}
