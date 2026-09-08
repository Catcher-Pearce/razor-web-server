package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.validation.RequestShape;

import java.util.List;

/**
 * A registered handler and the metadata needed to map requests for it.
 *
 * @param handler function invoked when this route matches
 * @param requestShape type used to deserialize and validate the body, or null
 *                     to preserve the raw string body
 * @param pathVariables declared variable names in route-segment order
 */
public record Route (
     Handler handler,
     Class<? extends RequestShape> requestShape,
     List<String> pathVariables
    ) {}
