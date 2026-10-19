package com.example.swd392_se1907_aives.modules.examination.internal.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class InterviewService {
    private final InterviewStateStore store;
    private final InterviewAiClient ai;
    private final InterviewAudioStorage audio;
    public InterviewStateStore.State state(UUID id){return store.state(id);}
    public InterviewStateStore.State textAnswer(UUID id,UUID promptId,String text){
        var plan=store.reserveAnswer(id,promptId,text.strip(),false);return continueAnswer(id,plan.turnId());
    }
    public InterviewStateStore.State audioAnswer(UUID id,UUID promptId,MultipartFile file){
        ai.requireConfigured();var content=audio.validate(file);var plan=store.reserveAnswer(id,promptId,null,true);
        try {
            String key=audio.save(id,plan.turnId(),content.bytes(),content.extension());store.audioSaved(id,plan.turnId(),key);
            store.transcribed(id,plan.turnId(),ai.transcribe(content.bytes(),content.mime(),content.extension()));
        }catch(RuntimeException e){store.transcriptionFailed(id,plan.turnId(),message(e));throw e;}
        return continueAnswer(id,plan.turnId());
    }
    public InterviewStateStore.State continueAnswer(UUID id,UUID answerId){
        InterviewStateStore.DecisionPlan plan;
        try {plan=store.claimDecision(id,answerId);}catch(ResponseStatusException e){
            if(e.getStatusCode().value()==409){var state=store.state(id);if(!"IN_PROGRESS".equals(state.status()))return state;}throw e;
        }
        if(plan!=null){
            try {store.decisionCompleted(id,plan,ai.followup(plan.context()));}
            catch(RuntimeException e){store.decisionFailed(id,plan.requestId(),message(e));}
        }
        return store.state(id);
    }
    public byte[] speech(UUID id,UUID turnId){
        var plan=store.claimSpeech(id,turnId);if(plan.cachedKey()!=null)return audio.read(plan.cachedKey());
        try {byte[] bytes=ai.speech(plan.text());String key=audio.save(id,turnId,bytes,"wav");store.speechFinished(id,plan,key,null);return bytes;}
        catch(RuntimeException e){store.speechFinished(id,plan,null,message(e));throw e;}
    }
    private String message(RuntimeException error){return error instanceof ResponseStatusException status?status.getReason():"Xử lý AI bị gián đoạn. Bạn có thể thử lại.";}
}
