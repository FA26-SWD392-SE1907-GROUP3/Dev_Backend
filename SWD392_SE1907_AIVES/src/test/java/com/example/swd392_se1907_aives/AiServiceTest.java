package com.example.swd392_se1907_aives;
import org.junit.jupiter.api.Test;
import com.example.swd392_se1907_aives.service.AiService;
import static org.junit.jupiter.api.Assertions.*;
class AiServiceTest {
 @Test void retrievalPrioritizesRelevantChunkRatherThanOnlyDocumentStart() {
  String text="unrelated introduction ".repeat(100)+"dependency inversion domain interface";
  assertTrue(new AiService().context(text,"dependency inversion").startsWith(text.substring(1200,Math.min(2800,text.length()))));
 }
}
