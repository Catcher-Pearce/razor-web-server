package com.catcher.miniserver.server;

import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.http.HttpMethod;
import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.http.HttpResponse;
import com.catcher.miniserver.exception.DuplicateRouteException;
import com.catcher.miniserver.exception.MethodNotAllowedException;
import com.catcher.miniserver.exception.RouteNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {
    Map<String, Map<HttpMethod, Route>> routeMap;
    RequestMapper requestMapper;

    public RequestDispatcher() {
        this.routeMap = new HashMap<>();
        this.requestMapper = new RequestMapper();
    }

    public void createRoute(String path, HttpMethod method, Handler handler, Class<? extends RequestShape> requestShape) {
        RoutePattern routePattern = new RoutePattern(path);
        ParsedRoute parsedRoute = routePattern.serializeRoute();

        Route route = new Route(
                handler,
                requestShape,
                parsedRoute.pathVariables()
        );

        if (!routeMap.containsKey(parsedRoute.path())) {
            routeMap.put(parsedRoute.path(), new HashMap<>(Map.of(method, route)));
        } else {
            if (routeMap.get(parsedRoute.path()).containsKey(method)) {
                throw new DuplicateRouteException(method, path);
            }
            routeMap.get(parsedRoute.path()).put(method, route);
        }
    }

    public HttpResponse handleRequest(HttpRequest request) {
        String[] requestRoute = request.path().split("/");
        String parsedRequestRoute = "";
        ArrayList<String> parsedPathVariables = new ArrayList<>();

        for (String route : routeMap.keySet()) {
            ArrayList<String> pathVariables = new ArrayList<String>();
            boolean matched = true;
            String[] mappedRoute = route.split("/");

            if (mappedRoute.length != requestRoute.length) {
                continue;
            }

            for (int i = 0; i < mappedRoute.length; i++) {
                if (!requestRoute[i].equals(mappedRoute[i]) && !mappedRoute[i].equals("{}")) {
                    matched = false;
                    break;
                } else if (mappedRoute[i].equals("{}")) {
                    pathVariables.add(requestRoute[i]);
                }
            }
            if (matched) {
                parsedRequestRoute = route;
                parsedPathVariables = pathVariables;
                break;
            }
        }

        if (parsedRequestRoute.isEmpty()) {
            throw new RouteNotFoundException(request.path());
        }

        Map<HttpMethod, Route> routesByMethod = routeMap.get(parsedRequestRoute);
        Route route = routesByMethod.get(request.method());
        if (route == null) {
            throw new MethodNotAllowedException(request.method(), request.path(), routesByMethod.keySet());
        }
        Map<String, String> pathVariablesMap = new HashMap<>();

        for (int i = 0; i < route.pathVariables().toArray().length; i++) {
            pathVariablesMap.put(route.pathVariables().get(i), parsedPathVariables.get(i));
        }

        ServerRequest serverRequest = requestMapper.map(route, request, pathVariablesMap);

        return route.handler().handle(serverRequest);
    }
}
