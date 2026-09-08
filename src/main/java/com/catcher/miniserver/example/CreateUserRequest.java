package com.catcher.miniserver.example;

import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.validation.annotations.Max;
import com.catcher.miniserver.validation.annotations.Min;
import com.catcher.miniserver.validation.annotations.NotNull;
import com.catcher.miniserver.validation.annotations.Size;

/**
 * JSON request body used by the example user-creation route.
 * Constraints are checked during request mapping before the handler runs.
 *
 * @param name required user name containing between 2 and 50 characters
 * @param age required age between 18 and 120, inclusive
 */
public record CreateUserRequest(
        @NotNull
        @Size(min = 2, max = 50)
        String name,

        @NotNull
        @Min(18)
        @Max(120)
        Integer age
) implements RequestShape {
}
