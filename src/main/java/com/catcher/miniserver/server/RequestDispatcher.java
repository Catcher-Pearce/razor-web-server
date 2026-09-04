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


/**
 * Registers application routes and dispatches incoming HTTP requests to their
 * handlers, with optional static-file fallback for unmatched GET requests.
 */
public class RequestDispatcher {
    private Map<String, Map<HttpMethod, Route>> routeMap;
    private RequestMapper requestMapper;
    private RouteMatcher routeMatcher;
    private StaticFileHandler staticFileHandler;

    public RequestDispatcher() {
        this.routeMap = new HashMap<>();
        this.requestMapper = new RequestMapper();
        this.routeMatcher = new RouteMatcher();
    }

    /**
     * Registers a route after validating and normalizing its path pattern.
     *
     * @param path raw path pattern specified by the user
     * @param method HTTP method accepted by the route
     * @param handler function that handles requests matching the route
     * @param requestShape optional type used to deserialize and validate request bodies
     * @throws DuplicateRouteException if the normalized path and method are already registered
     */
    public void createRoute(String path, HttpMethod method, Handler handler, Class<? extends RequestShape> requestShape) {
        RoutePattern routePattern = new RoutePattern(path);
        ParsedRoute parsedRoute = routePattern.normalizeRoute();

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
    /**
     * Dispatches a request to a registered route. When no route matches, an
     * unmatched GET request falls back to static-file serving if configured.
     * Other unmatched requests retain the original route-not-found behavior.
     *
     * @param request parsed incoming HTTP request
     * @return the response produced by a route handler or static-file handler
     */
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

    /**
     * Matches and maps a request, associates captured path values with their
     * declared variable names, and invokes the selected route handler.
     *
     * @param request parsed incoming HTTP request
     * @return the response produced by the matched route handler
     */
    public HttpResponse handleRegisteredRoute(HttpRequest request) {
        RouteMatch routeMatch = routeMatcher.match(
                routeMap,
                request.path(),
                request.method()
        );

        Route route = routeMatch.matchedRoute();
        Map<String, String> pathVariablesMap = new HashMap<>();
        Map<String, String> queryParams = routeMatch.queryParameters();
        for (int i = 0; i < route.pathVariables().toArray().length; i++) {
            pathVariablesMap.put(route.pathVariables().get(i), routeMatch.pathVariables().get(i));
        }

        ServerRequest serverRequest = requestMapper.map(route, request, pathVariablesMap, queryParams);

        return route.handler().handle(serverRequest);
    }

    /**
     * Configures the root directory used to serve unmatched GET requests.
     *
     * @param root static-file root directory
     */
    public void serveStaticFiles(Path root) {
        staticFileHandler = new StaticFileHandler(root);
    }
}
