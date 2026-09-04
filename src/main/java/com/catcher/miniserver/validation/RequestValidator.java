package com.catcher.miniserver.validation;

import com.catcher.miniserver.exception.RequestValidationException;
import com.catcher.miniserver.validation.annotations.Max;
import com.catcher.miniserver.validation.annotations.Min;
import com.catcher.miniserver.validation.annotations.NotNull;
import com.catcher.miniserver.validation.annotations.Size;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class RequestValidator {

    public void validate(RequestShape requestShape) {
        if (requestShape == null) {
            throw new IllegalArgumentException("Request shape cannot be null");
        }

        List<String> violations = new ArrayList<>();

        for (Class<?> type = requestShape.getClass();
             type != null && type != Object.class;
             type = type.getSuperclass()) {
            validateFields(type, requestShape, violations);
        }

        if (!violations.isEmpty()) {
            throw new RequestValidationException(violations);
        }
    }

    private void validateFields(
            Class<?> type,
            RequestShape requestShape,
            List<String> violations
    ) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(requestShape);

                validateNotNull(field, value, violations);
                validateMin(field, value, violations);
                validateMax(field, value, violations);
                validateSize(field, value, violations);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(
                        "Could not access field " + field.getName(),
                        exception
                );
            }
        }
    }

    private void validateNotNull(
            Field field,
            Object value,
            List<String> violations
    ) {
        if (field.isAnnotationPresent(NotNull.class) && value == null) {
            violations.add(field.getName() + " must not be null");
        }
    }

    private void validateMin(
            Field field,
            Object value,
            List<String> violations
    ) {
        Min min = field.getAnnotation(Min.class);
        if (min == null || value == null) {
            return;
        }

        BigDecimal numericValue = numericValue(field, value, Min.class);
        if (numericValue.compareTo(BigDecimal.valueOf(min.value())) < 0) {
            violations.add(field.getName() + " must be at least " + min.value());
        }
    }

    private void validateMax(
            Field field,
            Object value,
            List<String> violations
    ) {
        Max max = field.getAnnotation(Max.class);
        if (max == null || value == null) {
            return;
        }

        BigDecimal numericValue = numericValue(field, value, Max.class);
        if (numericValue.compareTo(BigDecimal.valueOf(max.value())) > 0) {
            violations.add(field.getName() + " must be at most " + max.value());
        }
    }

    private BigDecimal numericValue(
            Field field,
            Object value,
            Class<?> annotationType
    ) {
        if (!(value instanceof Number number)) {
            throw unsupportedType(field, annotationType, "a numeric field");
        }

        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "@" + annotationType.getSimpleName()
                            + " cannot validate non-finite value on field "
                            + field.getName(),
                    exception
            );
        }
    }

    private void validateSize(
            Field field,
            Object value,
            List<String> violations
    ) {
        Size size = field.getAnnotation(Size.class);
        if (size == null || value == null) {
            return;
        }

        if (size.min() < 0 || size.max() < size.min()) {
            throw new IllegalArgumentException(
                    "Invalid @Size bounds on field " + field.getName()
            );
        }

        int actualSize = sizeOf(field, value);
        if (actualSize < size.min() || actualSize > size.max()) {
            violations.add(
                    field.getName() + " size must be between "
                            + size.min() + " and " + size.max()
            );
        }
    }

    private int sizeOf(Field field, Object value) {
        if (value instanceof CharSequence sequence) {
            return sequence.length();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }

        throw unsupportedType(
                field,
                Size.class,
                "a string, collection, map, or array field"
        );
    }

    private IllegalArgumentException unsupportedType(
            Field field,
            Class<?> annotationType,
            String expectedType
    ) {
        return new IllegalArgumentException(
                "@" + annotationType.getSimpleName() + " on field "
                        + field.getName() + " requires " + expectedType
        );
    }
}
