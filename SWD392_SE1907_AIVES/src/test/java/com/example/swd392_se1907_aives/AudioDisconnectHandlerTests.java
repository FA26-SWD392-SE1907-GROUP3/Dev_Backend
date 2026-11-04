package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.bootstrap.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AudioDisconnectHandlerTests {
    @RestController static class AudioFixture {
        @GetMapping("/audio/abort") void aborted(HttpServletResponse response)throws IOException{
            response.setContentType("audio/wav");response.flushBuffer();
            throw new AsyncRequestNotUsableException("Fixture client disconnected");
        }
        @GetMapping("/audio/client-abort") void clientAbort(HttpServletResponse response)throws IOException{
            response.setContentType("audio/wav");throw new org.apache.catalina.connector.ClientAbortException("Fixture disconnected");
        }
        @GetMapping("/audio/failure") void failure(HttpServletResponse response){
            response.setContentType("audio/wav");throw new IllegalStateException("Fixture failure");
        }
        @GetMapping("/audio/io-error") void io(HttpServletResponse response)throws IOException{
            response.setContentType("audio/wav");throw new IOException("Fixture file error");
        }
    }
    @Test void disconnectedWavResponseDoesNotTryToSerializeJsonIntoAnUnusableConnection()throws Exception{
        var mvc=MockMvcBuilders.standaloneSetup(new AudioFixture()).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/audio/abort")).andExpect(content().string(""));
        mvc.perform(get("/audio/client-abort")).andExpect(content().string(""));
    }
    @Test void genuineErrorsStillReturnJsonEvenWhenThePreviousContentTypeWasAudio()throws Exception{
        var mvc=MockMvcBuilders.standaloneSetup(new AudioFixture()).setControllerAdvice(new GlobalExceptionHandler()).build();
        for(String path:new String[]{"/audio/failure","/audio/io-error"})
            mvc.perform(get(path)).andExpect(status().isInternalServerError()).andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.code").value(500));
    }
}
