package io.github.catcherpearce.razorserver.validation;

import io.github.catcherpearce.razorserver.exception.RequestValidationException;
import io.github.catcherpearce.razorserver.validation.annotations.Max;
import io.github.catcherpearce.razorserver.validation.annotations.Min;
import io.github.catcherpearce.razorserver.validation.annotations.NotNull;
import io.github.catcherpearce.razorserver.validation.annotations.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidatorTest {
    private final RequestValidator validator = new RequestValidator();

    @Test
    void acceptsAValidRequestShape() {
        ExampleRequest request = new ExampleRequest("Ada", 37, List.of("java"));

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void reportsAllConstraintViolations() {
        ExampleRequest request = new ExampleRequest(null, 12, List.of());

        RequestValidationException exception = assertThrows(
                RequestValidationException.class,
                () -> validator.validate(request)
        );

        assertEquals(List.of(
                "name must not be null",
                "age must be at least 18",
                "interests size must be between 1 and 3"
        ), exception.violations());
    }

    @Test
    void validatesMaximumValues() {
        ExampleRequest request = new ExampleRequest("Ada", 121, List.of("java"));

        RequestValidationException exception = assertThrows(
                RequestValidationException.class,
                () -> validator.validate(request)
        );

        assertEquals(List.of("age must be at most 120"), exception.violations());
    }

    @Test
    void constraintsOtherThanNotNullIgnoreNullValues() {
        NullableRequest request = new NullableRequest();

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void rejectsAnnotationsAppliedToUnsupportedTypes() {
        InvalidRequest request = new InvalidRequest();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );

        assertEquals(
                "@Min on field value requires a numeric field",
                exception.getMessage()
        );
    }

    @Test
    void rejectsANullRequestShape() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(null)
        );

        assertEquals("Request shape cannot be null", exception.getMessage());
    }

    private static final class ExampleRequest implements RequestShape {
        @NotNull
        @Size(min = 2, max = 50)
        private final String name;

        @Min(18)
        @Max(120)
        private final Integer age;

        @Size(min = 1, max = 3)
        private final List<String> interests;

        private ExampleRequest(String name, Integer age, List<String> interests) {
            this.name = name;
            this.age = age;
            this.interests = interests;
        }
    }

    private static final class NullableRequest implements RequestShape {
        @Min(1)
        private final Integer count = null;

        @Size(min = 1)
        private final String description = null;
    }

    private static final class InvalidRequest implements RequestShape {
        @Min(1)
        private final String value = "not numeric";
    }
}
