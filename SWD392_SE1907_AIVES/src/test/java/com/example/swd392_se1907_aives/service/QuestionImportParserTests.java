package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionImportParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
class QuestionImportParserTests {
    QuestionImportParser parser=new QuestionImportParser(new ObjectMapper());
    @Test void readsUtf8BomQuotedCommasEscapedQuotesAndMultilineContent(){
        String csv="\uFEFFcontent,referenceAnswer,bloomLevel,difficulty\r\n\"Nêu ví dụ, rồi giải thích \"\"vì sao\"\"\nở bước hai\",Đáp án,Hiểu,MEDIUM\r\n";
        var rows=parser.parse("questions.csv",csv.getBytes(StandardCharsets.UTF_8));assertThat(rows).hasSize(1);assertThat(rows.getFirst().content()).isEqualTo("Nêu ví dụ, rồi giải thích \"vì sao\"\nở bước hai");
    }
    @Test void rejectsMalformedCsvInvalidUtf8AndOversizedFiles(){
        for(String csv:new String[]{"content,bloomLevel,difficulty\n\"unclosed,APPLY,EASY","content,bloomLevel,difficulty\nx,APPLY","content,content,difficulty\nx,y,z"})
            assertThatThrownBy(()->parser.parse("q.csv",csv.getBytes(StandardCharsets.UTF_8))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->parser.parse("q.csv",new byte[]{(byte)0xC3,0x28})).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->parser.parse("q.csv",new byte[2*1024*1024+1])).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void rejectsTooManyRowsAndNonArrayJson(){
        assertThatThrownBy(()->parser.parse("q.json","{}".getBytes())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->parser.parse("q.json","[{}] garbage".getBytes())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        String json="["+String.join(",",java.util.Collections.nCopies(201,"{}"))+"]";
        assertThatThrownBy(()->parser.parse("q.json",json.getBytes())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
