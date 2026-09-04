package com.catcher.miniserver.example;

import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.validation.annotations.Max;
import com.catcher.miniserver.validation.annotations.Min;
import com.catcher.miniserver.validation.annotations.NotNull;
import com.catcher.miniserver.validation.annotations.Size;

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
