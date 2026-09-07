package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.DuplicateRouteException;
import com.catcher.miniserver.http.HttpMethod;

import java.util.*;

public class RouteNode {
    private RouteNode variableChild;
    private final Map<HttpMethod, Route> routes = new EnumMap<>(HttpMethod.class);
    private final Map<String, RouteNode> literalChildren = new HashMap<>();

    public RouteNode getOrCreateLiteralChild(String segment) {
        return literalChildren.computeIfAbsent(segment, key -> new RouteNode());
    }

    public RouteNode getOrCreateVariableChild() {
        if (variableChild == null) {
            variableChild = new RouteNode();
        }
        return variableChild;
    }

    public void addRoute(HttpMethod method, Route route, String path) {
        if (routes.putIfAbsent(method, route) != null) {
            throw new DuplicateRouteException(method, path);
        }
    }

    public RouteNode getLiteralChild(String segment) {
        return literalChildren.get(segment);
    }

    public RouteNode getVariableChild() {
       return variableChild;
    }

    public Route getRoute(HttpMethod method) {
        return routes.get(method);
    }
}