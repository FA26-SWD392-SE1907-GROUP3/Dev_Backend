package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.*;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ConversationAudioServiceImpl implements ConversationAudioService {
    private final InterviewStateStore store;
    private final InterviewAiClient ai;
    private final InterviewAudioStorage audio;
    private final LiveTranscriptionTickets tickets;
    private final Map<String,String> phrases;
    public ConversationAudioServiceImpl(InterviewStateStore store,InterviewAiClient ai,InterviewAudioStorage audio,
            LiveTranscriptionTickets tickets,ObjectMapper json)throws IOException {
        this.store=store;this.ai=ai;this.audio=audio;this.tickets=tickets;
        phrases=json.readValue(new ClassPathResource("prompts/interview-phrases.json").getContentAsString(StandardCharsets.UTF_8),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});
    }
    @Override public byte[] voice(UUID id,UUID promptId,Kind kind){
        try(var diagnosticScope=InterviewDiagnostics.scope(id,promptId,null)){
        var diagnostic=InterviewDiagnostics.start("voice.segment","speechKind",kind.name(),"source","live");
        var state=store.state(id); // Owner, enrolment and attempt are checked before spending API tokens.
        String text;
        if(kind==Kind.CLOSING){
            if(!"COMPLETED".equals(state.status()))throw conflict();
            text=state.turns().stream().filter(t->"SYSTEM_NOTE".equals(t.kind())).reduce((a,b)->b).map(t->t.text()+" ").orElse("")+phrases.get("closing");
        }else{
            if(!"IN_PROGRESS".equals(state.status())||state.prompt()==null||!state.prompt().id().equals(promptId))throw conflict();
            text=switch(kind){
                case GREETING -> phrases.get("greeting");
                case REMINDER -> phrases.get("reminder");
                case PROMPT, REPLAY -> {
                    String prefix="";
                    if(kind==Kind.PROMPT&&"MAIN_QUESTION".equals(state.prompt().kind())&&state.prompt().sequenceNo()>1)
                        prefix=state.turns().stream().filter(t->"SYSTEM_NOTE".equals(t.kind())&&t.sequenceNo()==state.prompt().sequenceNo()-1)
                                .reduce((a,b)->b).map(t->t.text()+" ").orElse("");
                    yield prefix+state.prompt().text();
                }
                default -> throw conflict();
            };
        }
        diagnostic.event("plan","textChars",text.length());
        byte[] result=ai.speech(text);
        diagnostic.event("ready","audioBytes",result.length);
        return result;
        }
    }
    @Override public TicketView ticket(UUID id,UUID promptId){
        ai.requireConfigured();
        return new TicketView(tickets.issue(store.conversationVoicePermission(id,promptId)),"/api/v1/interviews/live");
    }
    @Override public String transcribe(UUID id,UUID promptId,MultipartFile file){
        try(var diagnosticScope=InterviewDiagnostics.scope(id,promptId,null)){
        store.livePermission(id,promptId); // Only a currently answerable, owned prompt accepts recordings.
        ai.requireConfigured();
        var content=audio.validate(file);
        return ai.transcribe(content.bytes(),content.mime(),content.extension());
        }
    }
    private ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"Nội dung giọng nói không còn thuộc lượt trao đổi hiện tại.");}
}
