package io.github.catcherpearce.razorserver.exception;

/** Indicates invalid trusted-proxy configuration during server setup. */
public final class ProxyConfigurationException extends RuntimeException {
    public ProxyConfigurationException(String reason, Throwable cause) {
        super("Could not configure trusted proxies: " + reason
                + ". Use IPv4 or IPv6 CIDR notation, such as 192.168.1.0/24 or 2001:db8::/64."
                + " For a single address, add /32 for IPv4 (192.168.1.10/32)"
                + " or /128 for IPv6 (2001:db8::1/128).", cause);
    }
}
