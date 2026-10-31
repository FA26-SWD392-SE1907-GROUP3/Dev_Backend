package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradeValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AttemptGradeValidatorTests {
    private final ObjectMapper json=new ObjectMapper();
    private final AttemptGradeValidator validator=new AttemptGradeValidator();
    private final String rubric="""
        {"maxScore":10,"criteria":[
          {"sortOrder":1,"name":"Kiến thức toàn bài","maxPoints":6},
          {"sortOrder":2,"name":"Lập luận toàn bài","maxPoints":4}]}
        """;

    @Test void calculatesTotalFromEachCriterionOnceRatherThanTrustingAnAiTotal()throws Exception {
        var result=validator.validate(json.readTree(rubric),json.readTree("""
            {"totalScore":999,"feedback":"Nhận xét toàn bài","criteria":[
              {"sortOrder":2,"score":3.25,"feedback":"Lập luận có căn cứ"},
              {"sortOrder":1,"score":5,"feedback":"Đúng kiến thức"}]}
            """));
        assertThat(result.totalScore()).isEqualByComparingTo("8.25");
        assertThat(result.maxScore()).isEqualByComparingTo("10");
        assertThat(result.criteria()).extracting(criterion->criterion.sortOrder()).containsExactly(1,2);
    }

    @Test void rejectsDuplicateMissingUnknownAndOutOfRangeCriterionScores()throws Exception {
        for(String criteria:new String[]{
            "[{\"sortOrder\":1,\"score\":3,\"feedback\":\"a\"}]",
            "[{\"sortOrder\":1,\"score\":3,\"feedback\":\"a\"},{\"sortOrder\":1,\"score\":3,\"feedback\":\"b\"}]",
            "[{\"sortOrder\":1,\"score\":7,\"feedback\":\"a\"},{\"sortOrder\":2,\"score\":3,\"feedback\":\"b\"}]",
            "[{\"sortOrder\":1,\"score\":-1,\"feedback\":\"a\"},{\"sortOrder\":2,\"score\":3,\"feedback\":\"b\"}]",
            "[{\"sortOrder\":1,\"score\":3,\"feedback\":\"a\"},{\"sortOrder\":9,\"score\":3,\"feedback\":\"b\"}]",
            "[{\"sortOrder\":1,\"score\":3.001,\"feedback\":\"a\"},{\"sortOrder\":2,\"score\":3,\"feedback\":\"b\"}]"}) {
            var response=json.readTree("{\"feedback\":\"Toàn bài\",\"criteria\":"+criteria+"}");
            assertThatThrownBy(()->validator.validate(json.readTree(rubric),response)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
