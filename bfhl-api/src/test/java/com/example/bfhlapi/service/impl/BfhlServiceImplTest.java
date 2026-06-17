package com.example.bfhlapi.service.impl;

import com.example.bfhlapi.dto.BfhlRequest;
import com.example.bfhlapi.dto.BfhlResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class BfhlServiceImplTest {

    private final BfhlServiceImpl service = new BfhlServiceImpl();

    @Test
    void processHandlesMixedValuesDuplicatesAndInvalidEntries() {
        BfhlRequest request = BfhlRequest.builder()
                .data(Arrays.asList("A1B2", "100", "#", "Test123", "Z", "55", null, "", "   ", "100", "A1B2"))
                .build();

        BfhlResponse response = service.process(request, "REQ-1001");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getRequestId()).isEqualTo("REQ-1001");
        assertThat(response.getOddNumbers()).containsExactly("1", "123", "55");
        assertThat(response.getEvenNumbers()).containsExactly("2", "100");
        assertThat(response.getAlphabets()).containsExactly("A", "B", "Test", "Z");
        assertThat(response.getSpecialCharacters()).containsExactly("#");
        assertThat(response.getSum()).isEqualTo("281");
        assertThat(response.getLargestNumber()).isEqualTo("123");
        assertThat(response.getSmallestNumber()).isEqualTo("1");
        assertThat(response.getAlphabetCount()).isEqualTo(7);
        assertThat(response.getNumberCount()).isEqualTo(5);
        assertThat(response.getSpecialCharacterCount()).isEqualTo(1);
        assertThat(response.isContainsDuplicates()).isTrue();
        assertThat(response.getUniqueElementCount()).isEqualTo(6);
        assertThat(response.getSortedNumbers()).containsExactly("1", "2", "55", "100", "123");
        assertThat(response.getVowelCount()).isEqualTo(2);
        assertThat(response.getAlphabetFrequency()).containsExactly(
                entry("a", 1),
                entry("b", 1),
                entry("e", 1),
                entry("s", 1),
                entry("t", 2),
                entry("z", 1));
        assertThat(response.getLongestAlphabeticValue()).isEqualTo("Test");
        assertThat(response.getShortestAlphabeticValue()).isEqualTo("A");
        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(response.getSummary().getTotalElementsReceived()).isEqualTo(11);
        assertThat(response.getSummary().getValidElementsProcessed()).isEqualTo(6);
        assertThat(response.getSummary().getInvalidElementsIgnored()).isEqualTo(3);
    }

    @Test
    void processHandlesNegativeNumbersDecimalsAndEmbeddedSpecialCharacters() {
        BfhlRequest request = BfhlRequest.builder()
                .data(List.of("-3", "4.5", "A-2.50B", ".5", "0", "-4", "x@y"))
                .build();

        BfhlResponse response = service.process(request, "REQ-DECIMAL");

        assertThat(response.getOddNumbers()).containsExactly("-3");
        assertThat(response.getEvenNumbers()).containsExactly("0", "-4");
        assertThat(response.getSortedNumbers()).containsExactly("-4", "-3", "-2.5", "0", "0.5", "4.5");
        assertThat(response.getSum()).isEqualTo("-4.5");
        assertThat(response.getLargestNumber()).isEqualTo("4.5");
        assertThat(response.getSmallestNumber()).isEqualTo("-4");
        assertThat(response.getNumberCount()).isEqualTo(6);
        assertThat(response.getAlphabets()).containsExactly("A", "B", "x", "y");
        assertThat(response.getSpecialCharacters()).containsExactly("@");
        assertThat(response.getVowelCount()).isEqualTo(1);
        assertThat(response.isContainsDuplicates()).isFalse();
        assertThat(response.getSummary().getTotalElementsReceived()).isEqualTo(7);
        assertThat(response.getSummary().getValidElementsProcessed()).isEqualTo(7);
        assertThat(response.getSummary().getInvalidElementsIgnored()).isZero();
    }

    @Test
    void processReturnsEmptyNumericFieldsWhenOnlyInvalidValuesAreProvided() {
        BfhlRequest request = BfhlRequest.builder()
                .data(Arrays.asList(null, "", " ", "\t"))
                .build();

        BfhlResponse response = service.process(request, "REQ-EMPTY");

        assertThat(response.getOddNumbers()).isEmpty();
        assertThat(response.getEvenNumbers()).isEmpty();
        assertThat(response.getAlphabets()).isEmpty();
        assertThat(response.getSpecialCharacters()).isEmpty();
        assertThat(response.getSum()).isEmpty();
        assertThat(response.getLargestNumber()).isEmpty();
        assertThat(response.getSmallestNumber()).isEmpty();
        assertThat(response.getAlphabetFrequency()).isEmpty();
        assertThat(response.getUniqueElementCount()).isZero();
        assertThat(response.getSummary().getTotalElementsReceived()).isEqualTo(4);
        assertThat(response.getSummary().getValidElementsProcessed()).isZero();
        assertThat(response.getSummary().getInvalidElementsIgnored()).isEqualTo(4);
    }
}
