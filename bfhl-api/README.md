# bfhl-api

Spring Boot 3 REST API built with Java 17 and Maven.

## Endpoints

### GET /health

Response:

```json
{
  "status": "UP"
}
```

### POST /bfhl

Header:

```text
X-Request-Id: REQ-1001
```

Request:

```json
{
  "data": ["A1B2", "100", "#", "Test123", "Z", "55"]
}
```

Response fields:

```json
{
  "is_success": true,
  "request_id": "REQ-1001",
  "odd_numbers": [],
  "even_numbers": [],
  "alphabets": [],
  "special_characters": [],
  "sum": "",
  "largest_number": "",
  "smallest_number": "",
  "alphabet_count": 0,
  "number_count": 0,
  "special_character_count": 0,
  "contains_duplicates": false,
  "unique_element_count": 0,
  "sorted_numbers": [],
  "vowel_count": 0,
  "alphabet_frequency": {},
  "longest_alphabetic_value": "",
  "shortest_alphabetic_value": "",
  "processing_time_ms": 0,
  "summary": {
    "total_elements_received": 0,
    "valid_elements_processed": 0,
    "invalid_elements_ignored": 0
  }
}
```

## Processing Rules

- `data` is mandatory and accepts `List<String>`.
- Null values, empty strings, and whitespace-only strings are ignored.
- Duplicate values are removed before processing after trimming valid input values.
- `contains_duplicates` is true when duplicate valid values are found.
- Numeric parsing supports whole numbers, negative numbers, and decimal numbers.
- Decimal numbers are counted and sorted as numbers, but only whole numbers are classified as odd or even.
- Alphanumeric and mixed strings are decomposed into numeric sequences, alphabetic sequences, and special characters.
- Alphabet frequency is case-insensitive and returned with lowercase keys.
- `alphabet_count` counts alphabetic characters, not alphabetic sequence count.
- `number_count` counts numeric values found after extraction.
- `special_character_count` counts non-whitespace special characters.

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```

Run coverage verification:

```bash
mvn verify
```

The Maven build includes a JaCoCo rule requiring at least 80 percent line coverage for `BfhlServiceImpl`.

## Project Structure

```text
bfhl-api/
  pom.xml
  README.md
  src/
    main/
      java/
        com/example/bfhlapi/
          BfhlApiApplication.java
          controller/
            BfhlController.java
          dto/
            BfhlRequest.java
            BfhlResponse.java
            ErrorResponse.java
            HealthResponse.java
            ProcessingSummary.java
          exception/
            GlobalExceptionHandler.java
          service/
            BfhlService.java
            impl/
              BfhlServiceImpl.java
      resources/
        application.properties
    test/
      java/
        com/example/bfhlapi/
          controller/
            BfhlControllerTest.java
          service/
            impl/
              BfhlServiceImplTest.java
```
