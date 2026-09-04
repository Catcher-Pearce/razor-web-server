package com.catcher.miniserver.server;

import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.http.HttpMethod;

import java.util.Map;

public record ServerRequest(
        HttpMethod method,
        String path,
        Map<String, String> pathVariables,
        Map<String, String> queryParams,
        Map<String, String> headers,
        Object body
) {
    public <T> T bodyAs(Class<T> type) {
        return type.cast(body);
    }
}
