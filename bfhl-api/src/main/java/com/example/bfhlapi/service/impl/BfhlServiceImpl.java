package com.example.bfhlapi.service.impl;

import com.example.bfhlapi.dto.BfhlRequest;
import com.example.bfhlapi.dto.BfhlResponse;
import com.example.bfhlapi.dto.ProcessingSummary;
import com.example.bfhlapi.service.BfhlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BfhlServiceImpl implements BfhlService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[+-]?(?:(?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))");
    private static final Pattern ALPHABET_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final BigInteger TWO = BigInteger.valueOf(2);

    @Override
    public BfhlResponse process(BfhlRequest request, String requestId) {
        long startedAt = System.nanoTime();
        List<String> input = request.getData();

        int totalElementsReceived = input == null ? 0 : input.size();
        int invalidElementsIgnored = 0;
        boolean containsDuplicates = false;
        Set<String> uniqueValues = new LinkedHashSet<>();

        if (input != null) {
            for (String rawValue : input) {
                if (rawValue == null || rawValue.trim().isEmpty()) {
                    invalidElementsIgnored++;
                    continue;
                }

                String value = rawValue.trim();
                if (!uniqueValues.add(value)) {
                    containsDuplicates = true;
                }
            }
        }

        ProcessingAccumulator accumulator = new ProcessingAccumulator();
        for (String value : uniqueValues) {
            processValue(value, accumulator);
        }

        List<String> sortedNumbers = accumulator.numbers.stream()
                .sorted(Comparator.comparing(ProcessedNumber::value))
                .map(ProcessedNumber::displayValue)
                .toList();

        String sum = accumulator.numbers.isEmpty()
                ? ""
                : normalizeNumber(accumulator.sum);

        String largestNumber = accumulator.numbers.stream()
                .max(Comparator.comparing(ProcessedNumber::value))
                .map(ProcessedNumber::value)
                .map(BfhlServiceImpl::normalizeNumber)
                .orElse("");

        String smallestNumber = accumulator.numbers.stream()
                .min(Comparator.comparing(ProcessedNumber::value))
                .map(ProcessedNumber::value)
                .map(BfhlServiceImpl::normalizeNumber)
                .orElse("");

        long processingTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        log.atInfo()
                .addKeyValue("requestId", requestId)
                .addKeyValue("uniqueElementCount", uniqueValues.size())
                .addKeyValue("numberCount", accumulator.numbers.size())
                .addKeyValue("alphabetCount", accumulator.alphabetCount)
                .addKeyValue("specialCharacterCount", accumulator.specialCharacters.size())
                .log("BFHL request processed");

        return BfhlResponse.builder()
                .success(true)
                .requestId(requestId)
                .oddNumbers(accumulator.oddNumbers)
                .evenNumbers(accumulator.evenNumbers)
                .alphabets(accumulator.alphabets)
                .specialCharacters(accumulator.specialCharacters)
                .sum(sum)
                .largestNumber(largestNumber)
                .smallestNumber(smallestNumber)
                .alphabetCount(accumulator.alphabetCount)
                .numberCount(accumulator.numbers.size())
                .specialCharacterCount(accumulator.specialCharacters.size())
                .containsDuplicates(containsDuplicates)
                .uniqueElementCount(uniqueValues.size())
                .sortedNumbers(sortedNumbers)
                .vowelCount(accumulator.vowelCount)
                .alphabetFrequency(accumulator.alphabetFrequency)
                .longestAlphabeticValue(accumulator.longestAlphabeticValue)
                .shortestAlphabeticValue(accumulator.shortestAlphabeticValue)
                .processingTimeMs(processingTimeMs)
                .summary(ProcessingSummary.builder()
                        .totalElementsReceived(totalElementsReceived)
                        .validElementsProcessed(uniqueValues.size())
                        .invalidElementsIgnored(invalidElementsIgnored)
                        .build())
                .build();
    }

    private void processValue(String value, ProcessingAccumulator accumulator) {
        if (isFullNumber(value)) {
            addNumber(value, accumulator);
            return;
        }

        boolean[] consumedByNumber = new boolean[value.length()];
        extractNumbers(value, accumulator, consumedByNumber);
        extractAlphabets(value, accumulator);
        extractSpecialCharacters(value, consumedByNumber, accumulator);
    }

    private boolean isFullNumber(String value) {
        return NUMBER_PATTERN.matcher(value).matches();
    }

    private void extractNumbers(String value, ProcessingAccumulator accumulator, boolean[] consumedByNumber) {
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        while (matcher.find()) {
            String numberToken = matcher.group();
            addNumber(numberToken, accumulator);
            for (int index = matcher.start(); index < matcher.end(); index++) {
                consumedByNumber[index] = true;
            }
        }
    }

    private void extractAlphabets(String value, ProcessingAccumulator accumulator) {
        Matcher matcher = ALPHABET_PATTERN.matcher(value);
        while (matcher.find()) {
            addAlphabeticValue(matcher.group(), accumulator);
        }
    }

    private void extractSpecialCharacters(String value, boolean[] consumedByNumber, ProcessingAccumulator accumulator) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!Character.isLetterOrDigit(current) && !Character.isWhitespace(current) && !consumedByNumber[index]) {
                accumulator.specialCharacters.add(String.valueOf(current));
            }
        }
    }

    private void addNumber(String numberToken, ProcessingAccumulator accumulator) {
        BigDecimal parsedNumber = new BigDecimal(numberToken);
        String displayValue = normalizeNumber(parsedNumber);
        accumulator.numbers.add(new ProcessedNumber(parsedNumber, displayValue));
        accumulator.sum = accumulator.sum.add(parsedNumber);

        if (isWholeNumber(parsedNumber)) {
            BigInteger integerValue = parsedNumber.toBigIntegerExact();
            if (integerValue.remainder(TWO).abs().equals(BigInteger.ZERO)) {
                accumulator.evenNumbers.add(displayValue);
            } else {
                accumulator.oddNumbers.add(displayValue);
            }
        }
    }

    private void addAlphabeticValue(String alphabeticValue, ProcessingAccumulator accumulator) {
        accumulator.alphabets.add(alphabeticValue);

        if (accumulator.longestAlphabeticValue.isEmpty()
                || alphabeticValue.length() > accumulator.longestAlphabeticValue.length()) {
            accumulator.longestAlphabeticValue = alphabeticValue;
        }

        if (accumulator.shortestAlphabeticValue.isEmpty()
                || alphabeticValue.length() < accumulator.shortestAlphabeticValue.length()) {
            accumulator.shortestAlphabeticValue = alphabeticValue;
        }

        for (char rawCharacter : alphabeticValue.toCharArray()) {
            char lowerCharacter = Character.toLowerCase(rawCharacter);
            String key = String.valueOf(lowerCharacter);
            accumulator.alphabetFrequency.merge(key, 1, Integer::sum);
            accumulator.alphabetCount++;
            if (isVowel(lowerCharacter)) {
                accumulator.vowelCount++;
            }
        }
    }

    private static boolean isWholeNumber(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private static boolean isVowel(char value) {
        return value == 'a' || value == 'e' || value == 'i' || value == 'o' || value == 'u';
    }

    private static String normalizeNumber(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return normalized.toPlainString();
    }

    private record ProcessedNumber(BigDecimal value, String displayValue) {
    }

    private static class ProcessingAccumulator {
        private final List<ProcessedNumber> numbers = new ArrayList<>();
        private final List<String> oddNumbers = new ArrayList<>();
        private final List<String> evenNumbers = new ArrayList<>();
        private final List<String> alphabets = new ArrayList<>();
        private final List<String> specialCharacters = new ArrayList<>();
        private final Map<String, Integer> alphabetFrequency = new TreeMap<>();
        private BigDecimal sum = BigDecimal.ZERO;
        private int alphabetCount;
        private int vowelCount;
        private String longestAlphabeticValue = "";
        private String shortestAlphabeticValue = "";
    }
}
