package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;


import org.springframework.stereotype.Service;
import java.util.*;

/** Adaptive mode balances exposure and Bloom/difficulty coverage; it does not infer student ability. */
@Service
public class QuestionSetSelector {
    public List<QuestionInfo> select(List<QuestionInfo> eligible,int count,String mode,Set<UUID> previous,Map<UUID,Integer> usage){
        if(count<1||eligible.size()<count)throw new IllegalArgumentException("Insufficient approved questions");
        var remaining=new ArrayList<>(eligible);Collections.shuffle(remaining);List<QuestionInfo> selected=new ArrayList<>();
        if(!"ADAPTIVE".equals(mode)){
            remaining.sort(Comparator.comparing(q->previous.contains(q.getId())));
            return new ArrayList<>(remaining.subList(0,count));
        }
        Map<String,Integer> blooms=new HashMap<>(),difficulties=new HashMap<>();
        for(int i=0;i<count;i++){
            // Stable sort retains random tie breaks. Avoid the previous set first, then balance recent exposure and coverage.
            remaining.sort(Comparator.<QuestionInfo,Boolean>comparing(q->previous.contains(q.getId()))
                .thenComparingInt(q->usage.getOrDefault(q.getId(),0))
                .thenComparingInt(q->blooms.getOrDefault(q.getBloomLevel(),0))
                .thenComparingInt(q->difficulties.getOrDefault(q.getDifficulty(),0)));
            QuestionInfo q=remaining.removeFirst();selected.add(q);blooms.merge(q.getBloomLevel(),1,Integer::sum);difficulties.merge(q.getDifficulty(),1,Integer::sum);
        }
        return selected;
    }
}
