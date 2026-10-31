package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSetSelector;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class QuestionSetSelectorTests {
    QuestionSetSelector selector=new QuestionSetSelector(List.of(
            new com.example.swd392_se1907_aives.modules.examination.internal.service.impl.RandomQuestionSelectionImpl(),
            new com.example.swd392_se1907_aives.modules.examination.internal.service.impl.AdaptiveQuestionSelectionImpl()));
    @Test void bothModesAvoidImmediateRepeatsAndNeverDuplicateWithinOneSet(){
        var list=List.of(q("REMEMBER","EASY"),q("UNDERSTAND","MEDIUM"),q("APPLY","HARD"),q("ANALYZE","HARD"));
        for(String mode:List.of("RANDOM","ADAPTIVE"))for(int i=0;i<20;i++){
            var selected=selector.select(list,2,mode,Set.of(list.get(0).getId(),list.get(1).getId()),Map.of());
            assertThat(selected).containsExactlyInAnyOrder(list.get(2),list.get(3));
        }
    }
    @Test void shortPoolsUseOnlyUnavoidableOverlap(){
        var list=List.of(q("REMEMBER","EASY"),q("UNDERSTAND","MEDIUM"),q("APPLY","HARD"));
        for(String mode:List.of("RANDOM","ADAPTIVE")){
            var selected=selector.select(list,2,mode,Set.of(list.get(0).getId(),list.get(1).getId()),Map.of());
            assertThat(selected).hasSize(2).contains(list.get(2));assertThat(selected.stream().map(QuestionInfo::getId).distinct()).hasSize(2);
        }
    }
    @Test void adaptivePrefersLessExposedQuestionsAndDiversifiesDifficultyOnTies(){
        var high=q("REMEMBER","EASY");var one=q("UNDERSTAND","MEDIUM");var two=q("APPLY","HARD");
        assertThat(selector.select(List.of(high,one,two),2,"ADAPTIVE",Set.of(),Map.of(high.getId(),9))).containsExactlyInAnyOrder(one,two);
        var same=q("UNDERSTAND","MEDIUM");
        for(int i=0;i<20;i++)assertThat(selector.select(List.of(one,same,two),2,"ADAPTIVE",Set.of(),Map.of()).stream().map(QuestionInfo::getDifficulty).distinct()).hasSize(2);
    }
    private QuestionInfo q(String bloom,String difficulty){return new QuestionInfo(UUID.randomUUID(),null,null,null,difficulty,null);}
}
