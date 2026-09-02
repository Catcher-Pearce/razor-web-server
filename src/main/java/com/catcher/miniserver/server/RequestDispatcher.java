package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.RouteNotFoundException;
import com.catcher.miniserver.file_serving.StaticFileHandler;
import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.http.HttpMethod;
import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.http.HttpResponse;
import com.catcher.miniserver.exception.DuplicateRouteException;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {
    Map<String, Map<HttpMethod, Route>> routeMap;
    RequestMapper requestMapper;
    RouteMatcher routeMatcher;
    StaticFileHandler staticFileHandler;

    public RequestDispatcher() {
        this.routeMap = new HashMap<>();
        this.requestMapper = new RequestMapper();
        this.routeMatcher = new RouteMatcher();
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
        try {
            return handleRegisteredRoute(request);
        } catch (RouteNotFoundException exception) {
            if (staticFileHandler != null && request.method() == HttpMethod.GET) {
                return staticFileHandler.serve(request.path());
            }

            throw exception;
        }
    }

    public HttpResponse handleRegisteredRoute(HttpRequest request) {
        RouteMatch routeMatch = routeMatcher.match(
                routeMap,
                request.path(),
                request.method()
        );

        Route route = routeMatch.matchedRoute();
        Map<String, String> pathVariablesMap = new HashMap<>();

        for (int i = 0; i < route.pathVariables().toArray().length; i++) {
            pathVariablesMap.put(route.pathVariables().get(i), routeMatch.pathVariables().get(i));
        }

        ServerRequest serverRequest = requestMapper.map(route, request, pathVariablesMap);

        return route.handler().handle(serverRequest);
    }

    public void serveStaticFiles(Path root) {
        staticFileHandler = new StaticFileHandler(root);
    }
}
