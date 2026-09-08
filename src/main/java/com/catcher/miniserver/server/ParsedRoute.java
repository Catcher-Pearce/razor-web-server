package com.catcher.miniserver.server;

import java.util.List;

/**
 * A validated registration pattern split into normalized path segments.
 *
 * @param path literal segments and {@code {}} placeholders for variables;
 *             empty for the root route
 * @param pathVariables variable names in path order, corresponding to the
 *                      placeholders in {@code path}
 */
public record ParsedRoute(
        List<String> path,
        List<String> pathVariables
) {
}