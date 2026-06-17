package com.example.bfhlapi.controller;

import com.example.bfhlapi.dto.BfhlRequest;
import com.example.bfhlapi.dto.BfhlResponse;
import com.example.bfhlapi.dto.ProcessingSummary;
import com.example.bfhlapi.exception.GlobalExceptionHandler;
import com.example.bfhlapi.service.BfhlService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BfhlController.class)
@Import(GlobalExceptionHandler.class)
class BfhlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BfhlService bfhlService;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void processDelegatesToServiceAndReturnsResponse() throws Exception {
        BfhlResponse serviceResponse = BfhlResponse.builder()
                .success(true)
                .requestId("REQ-1001")
                .oddNumbers(List.of("1"))
                .evenNumbers(List.of("100"))
                .alphabets(List.of("A", "B"))
                .specialCharacters(List.of("#"))
                .sum("101")
                .largestNumber("100")
                .smallestNumber("1")
                .alphabetCount(2)
                .numberCount(2)
                .specialCharacterCount(1)
                .containsDuplicates(false)
                .uniqueElementCount(2)
                .sortedNumbers(List.of("1", "100"))
                .vowelCount(1)
                .alphabetFrequency(Map.of("a", 1, "b", 1))
                .longestAlphabeticValue("A")
                .shortestAlphabeticValue("A")
                .processingTimeMs(3)
                .summary(ProcessingSummary.builder()
                        .totalElementsReceived(2)
                        .validElementsProcessed(2)
                        .invalidElementsIgnored(0)
                        .build())
                .build();

        when(bfhlService.process(any(BfhlRequest.class), eq("REQ-1001"))).thenReturn(serviceResponse);

        mockMvc.perform(post("/bfhl")
                        .header("X-Request-Id", "REQ-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":[\"A1B2\",\"100\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.request_id").value("REQ-1001"))
                .andExpect(jsonPath("$.odd_numbers[0]").value("1"))
                .andExpect(jsonPath("$.even_numbers[0]").value("100"))
                .andExpect(jsonPath("$.summary.total_elements_received").value(2));

        ArgumentCaptor<BfhlRequest> requestCaptor = ArgumentCaptor.forClass(BfhlRequest.class);
        verify(bfhlService).process(requestCaptor.capture(), eq("REQ-1001"));
        assertThat(requestCaptor.getValue().getData()).containsExactly("A1B2", "100");
    }

    @Test
    void processReturnsBadRequestWhenDataFieldIsMissing() throws Exception {
        mockMvc.perform(post("/bfhl")
                        .header("X-Request-Id", "REQ-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.request_id").value("REQ-1001"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validation_errors.data").value("data field is mandatory"));
    }

    @Test
    void processReturnsBadRequestWhenRequestIdHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/bfhl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":[\"100\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.message").value("X-Request-Id header is mandatory"));
    }
}
