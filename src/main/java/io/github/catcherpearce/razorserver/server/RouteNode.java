package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.exception.DuplicateRouteException;
import io.github.catcherpearce.razorserver.http.HttpMethod;

import java.util.*;

/**
 * A singular node within the route tree.
 *
 * <p>Stores literal children by segment name, routes by HTTP method, and
 * an optional variable child shared by variable segments at this position.</p>
 */
public class RouteNode {
    private RouteNode variableChild;
    private final Map<HttpMethod, Route> routes = new EnumMap<>(HttpMethod.class);
    private final Map<String, RouteNode> literalChildren = new HashMap<>();

    /**
     * Retrieves or creates a new literal child dependent on whether
     * a child already exists with that segment name.
     *
     * @param segment The name of the segment to create or retrieve.
     * @return The newly created or already existing RouteNode.
     */
    public RouteNode getOrCreateLiteralChild(String segment) {
        return literalChildren.computeIfAbsent(segment, _ -> new RouteNode());
    }

    /**
     * Retrieves the variable child, creating it if it does not yet exist.
     *
     * @return The newly created or already existing RouteNode.
     */
    public RouteNode getOrCreateVariableChild() {
        if (variableChild == null) {
            variableChild = new RouteNode();
        }
        return variableChild;
    }

    /**
     * Adds a new method-specific route to this RouteNode.
     *
     * @param method HTTP method accepted by the route
     * @param route route to register at this node
     * @param path route path used in the duplicate-route error message
     * @throws DuplicateRouteException if a route for the method already exists;
     *         the existing route is retained
     */
    public void addRoute(HttpMethod method, Route route, String path) {
        if (routes.putIfAbsent(method, route) != null) {
            throw new DuplicateRouteException(method, path);
        }
    }

    /**
     * Retrieves the literal child for a segment without creating one.
     *
     * @param segment segment name to look up
     * @return the matching child, or {@code null} if none exists
     */
    public RouteNode getLiteralChild(String segment) {
        return literalChildren.get(segment);
    }

    /**
     * Retrieves the variable child without creating one.
     *
     * @return the variable child, or {@code null} if none exists
     */
    public RouteNode getVariableChild() {
       return variableChild;
    }

    /**
     * Retrieves the route registered at this node for an HTTP method.
     *
     * @param method HTTP method to look up
     * @return the registered route, or {@code null} if none exists
     */
    public Route getRoute(HttpMethod method) {
        return routes.get(method);
    }

    /**
     * Retrieves the HTTP methods registered at this node.
     *
     * @return an immutable snapshot of the allowed methods
     */
    public Set<HttpMethod> getAllowedMethods() {
        return Set.copyOf(routes.keySet());
    }
}
