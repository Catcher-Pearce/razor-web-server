package com.catcher.miniserver.validation;

import com.catcher.miniserver.exception.RequestBodyDeserializationException;
import com.catcher.miniserver.exception.UnsupportedMediaTypeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestDeserializerTest {
    private final RequestDeserializer deserializer = new RequestDeserializer();

    public record CreateUserRequest(String name, Integer age) implements RequestShape {}

    @Test
    public void testValidJson() {
        RequestShape result = deserializer.deserialize(
                """
                        {"name": "john doe", "age": 21}
                        """,
                "application/json",
                CreateUserRequest.class
        );

        CreateUserRequest request =
                assertInstanceOf(CreateUserRequest.class, result);

        assertEquals("john doe", request.name());
        assertEquals(Integer.valueOf(21), request.age());
    }

    @Test
    public void testInvalidJson() {
        assertThrows(RequestBodyDeserializationException.class, () -> deserializer.deserialize(
                """
                        {name: "john doe", "age": 21}
                        """,
                "application/json",
                CreateUserRequest.class
        ));
    }

    @Test
    public void testMissingContentType() {
        assertThrows(UnsupportedMediaTypeException.class, () -> deserializer.deserialize(
                """
                        {"name": "john doe", "age": 21}
                        """,
                null,
                CreateUserRequest.class
        ));
    }

    @Test
    public void testUnsupportedContentType() {
        assertThrows(UnsupportedMediaTypeException.class, () -> deserializer.deserialize(
                """
                        {"name": "john doe", "age": 21}
                        """,
                "text/plain",
                CreateUserRequest.class
        ));
    }

    @Test
    public void testIncompatibleFieldType() {
        assertThrows(RequestBodyDeserializationException.class, () -> deserializer.deserialize(
                """
                        {"name": "john doe", "age": "abc"}
                        """,
                "application/json",
                CreateUserRequest.class
        ));
    }

    @Test
    public void testEmptyBody() {
        assertThrows(RequestBodyDeserializationException.class, () -> deserializer.deserialize(
                "",
                "application/json",
                CreateUserRequest.class
        ));
    }

    @Test
    public void testJsonWithCharset() {
        RequestShape result = deserializer.deserialize(
                """
                        {"name": "john doe", "age": 21}
                        """,
                "application/json; charset=UTF-8",
                CreateUserRequest.class
        );

        CreateUserRequest request =
                assertInstanceOf(CreateUserRequest.class, result);

        assertEquals("john doe", request.name());
        assertEquals(Integer.valueOf(21), request.age());
    }

    @Test
    public void testNullRequestShape() {
        assertNull(deserializer.deserialize(
                """
                        {"name": "john doe", "age": 21}
                        """,
                "application/json",
                null
        ));
    }
}
