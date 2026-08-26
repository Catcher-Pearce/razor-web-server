package com.catcher.miniserver.server;

import com.catcher.miniserver.validation.RequestShape;

import java.util.List;

public record Route (
     Handler handler,
     Class<? extends RequestShape> requestShape,
     List<String> pathVariables
    ) {}
