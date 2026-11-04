package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Observation only: never logs transcripts, prompts, answer keys, credentials or audio. */
@Component
public final class InterviewDiagnostics {
    private static final Logger LOG=LoggerFactory.getLogger(InterviewDiagnostics.class);
    private static final ObjectMapper JSON=new ObjectMapper();
    private static volatile boolean enabled;
    private static final Set<String> IDS=Set.of("attemptId","promptId","answerId");
    private static final Set<String> FIELDS=Set.of("attemptId","promptId","answerId","operationId","speechKind","source","model","voiceId",
            "speed","pitch","sampleRate","format","elapsedMs","firstTokenMs","firstByteMs","status","finishReason",
            "rawChars","messageChars","inputChars","audioBytes","transcriptChars","attempt","timeoutMs","maxOutputTokens",
            "reasoning","decision","reason","cacheHit","textChars","errorType","eventType","deadlinePresent","followupsUsed",
            "maxFollowups","answerLimitSeconds","historyChars","httpStatus","sinceCommitMs","sinceConnectMs","responseBytes","closeCode");

    public InterviewDiagnostics(@Value("${app.interview.diagnostics-enabled:false}") boolean active){enabled=active;}
    public static boolean enabled(){return enabled;}
    public static Span start(String operation,Object...fields){return new Span(operation,fields);}
    public static void event(String stage,Object...fields){emit(stage,null,fields);}
    static Map<String,Object> record(String stage,String operationId,Object...fields){
        var record=new LinkedHashMap<String,Object>();
        record.put("event",stage);record.put("timestamp",Instant.now().toString());record.put("sourceRuntime","backend");
        for(String key:IDS){String value=MDC.get("interview."+key);if(value!=null)record.put(key,value);}
        if(operationId!=null)record.put("operationId",operationId);
        for(int i=0;i+1<fields.length;i+=2){
            String key=String.valueOf(fields[i]);Object value=fields[i+1];
            if(!FIELDS.contains(key)||value==null)continue;
            if(value instanceof UUID)record.put(key,value.toString());
            else if(value instanceof Number||value instanceof Boolean)record.put(key,value);
            else if(value instanceof String s&&s.length()<=96&&s.matches("[A-Za-z0-9_.:/ -]*")&&!s.contains("sk-")&&!s.toLowerCase(Locale.ROOT).contains("bearer"))record.put(key,s);
        }
        return record;
    }
    private static void emit(String stage,String operationId,Object...fields){
        if(!enabled)return;
        try{LOG.info("INTERVIEW_DIAG {}",JSON.writeValueAsString(record(stage,operationId,fields)));}
        catch(RuntimeException|java.io.IOException ignored){/* Observability must never break an exam. */}
    }
    public static final class Span {
        private final String operation,id=UUID.randomUUID().toString();
        private final long started=System.nanoTime();
        private final Object[] fields;
        private final Map<String,String> context=InterviewDiagnostics.context();
        Span(String operation,Object...fields){this.operation=operation;this.fields=fields;emit(operation+".start",id,fields);}
        public long elapsedMs(){return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);}
        public void event(String suffix,Object...extra){
            var all=new ArrayList<Object>(Arrays.asList(fields));all.add("elapsedMs");all.add(elapsedMs());all.addAll(Arrays.asList(extra));
            try(var ignored=restore(context)){emit(operation+"."+suffix,id,all.toArray());}catch(RuntimeException ignored){}
        }
    }
    public static Scope scope(UUID attemptId,UUID promptId,UUID answerId){
        var next=new HashMap<String,String>();if(attemptId!=null)next.put("interview.attemptId",attemptId.toString());
        if(promptId!=null)next.put("interview.promptId",promptId.toString());if(answerId!=null)next.put("interview.answerId",answerId.toString());
        return restore(next);
    }
    public static Map<String,String> context(){
        var result=new HashMap<String,String>();for(String key:IDS){String value=MDC.get("interview."+key);if(value!=null)result.put("interview."+key,value);}return result;
    }
    public static Scope restore(Map<String,String> values){return new Scope(values);}
    public static <T> java.net.http.HttpResponse.BodyHandler<T> observe(java.net.http.HttpResponse.BodyHandler<T> delegate,Span trace){
        return info->{
            var downstream=delegate.apply(info);
            return new java.net.http.HttpResponse.BodySubscriber<T>(){
                private boolean first=true;
                public java.util.concurrent.CompletionStage<T> getBody(){return downstream.getBody();}
                public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){downstream.onSubscribe(subscription);}
                public void onNext(List<java.nio.ByteBuffer> buffers){
                    if(first&&buffers.stream().anyMatch(java.nio.ByteBuffer::hasRemaining)){first=false;trace.event("first_byte","firstByteMs",trace.elapsedMs());}
                    downstream.onNext(buffers);
                }
                public void onError(Throwable error){downstream.onError(error);}
                public void onComplete(){downstream.onComplete();}
            };
        };
    }
    public static final class Scope implements AutoCloseable {
        private final Map<String,String> before=MDC.getCopyOfContextMap();
        Scope(Map<String,String> values){values.forEach(MDC::put);}
        @Override public void close(){if(before==null)MDC.clear();else MDC.setContextMap(before);}
    }
}
