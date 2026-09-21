package io.github.catcherpearce.razorserver.http;

import io.github.catcherpearce.razorserver.exception.ProxyConfigurationException;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils6;

import java.net.InetAddress;
import java.net.Inet6Address;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configures trusted IPv4 and IPv6 proxy subnets and resolves client IPs from
 * the request's X-Forwarded-For header.
 */
public class ProxyHandler {
    private final Set<SubnetUtils> allowedSubnets = new HashSet<>();
    private final Set<SubnetUtils6> allowedIpv6Subnets = new HashSet<>();

    /**
     * Validates and creates trusted IPv4 and IPv6 subnets. Single-address
     * subnets use /32 for IPv4 or /128 for IPv6. IPv4-mapped IPv6 subnets
     * with prefixes from /96 through /128 are normalized to IPv4 subnets.
     *
     * @param subnetStrings List of String subnets provided by the user.
     * @throws ProxyConfigurationException if a subnet is invalid or the list is null
     */
    public ProxyHandler(List<String> subnetStrings) {
        if (subnetStrings == null) {
            throw new ProxyConfigurationException("the subnet list must not be null", null);
        }
        for (String subnet : subnetStrings) {
            try {
                if (subnet == null || subnet.isBlank()) {
                    throw new IllegalArgumentException("Subnet must not be null or blank");
                }
                int slash = subnet.indexOf('/');
                if (slash < 0) {
                    throw new IllegalArgumentException("Subnet requires a CIDR prefix");
                }

                String addressText = subnet.substring(0, slash);
                InetAddress address = InetAddress.ofLiteral(addressText);
                int prefix = Integer.parseInt(subnet.substring(slash + 1));
                if (address instanceof Inet6Address) {
                    allowedIpv6Subnets.add(new SubnetUtils6(address.getHostAddress(), prefix));
                } else {
                    if (addressText.contains(":")) {
                        if (prefix < 96 || prefix > 128) {
                            throw new IllegalArgumentException("IPv4-mapped IPv6 prefixes must be between 96 and 128");
                        }
                        prefix -= 96;
                    }
                    SubnetUtils trustedSubnet = new SubnetUtils(addressText.contains(":")
                            ? address.getHostAddress() + "/" + prefix : subnet);
                    trustedSubnet.setInclusiveHostCount(true);
                    allowedSubnets.add(trustedSubnet);
                }
            } catch (IllegalArgumentException exception) {
                throw new ProxyConfigurationException("invalid subnet '" + subnet + "'", exception);
            }
        }
    }

    /**
     * Resolves the client IP by scanning the X-Forwarded-For addresses from right
     * to left, skipping trusted proxies. Returns the first untrusted address
     * encountered, or the leftmost address if every address is trusted.
     * Call this only after verifying that the connection's remote IP is trusted
     * with {@link #checkIp(String)}.
     *
     * @param ipAddresses nonempty list of trimmed IPv4 or IPv6 literals in header order
     * @return the rightmost untrusted address, or the leftmost address if all are trusted
     * @throws java.util.NoSuchElementException if the list is empty
     * @throws IllegalArgumentException if an inspected address is not a valid IP literal
     */
    public String findClientIp(List<String> ipAddresses) {
        for (int i = ipAddresses.size() - 1; i >= 0; i--) {
            if (!checkIp(ipAddresses.get(i))) {
                return ipAddresses.get(i);
            }
        }
        return ipAddresses.getFirst();
    }

    /**
     * Checks whether the ip belongs to a configured trusted
     * proxy subnet, including its network and broadcast addresses. Use this
     * check before trusting the request's X-Forwarded-For header.
     *
     * IPv4-mapped IPv6 literals are matched against IPv4 subnets.
     * No hostname lookups are performed.
     *
     * @param ip IPv4 or IPv6 literal
     * @return {@code true} if the address belongs to any trusted subnet;
     *         {@code false} otherwise, including when no subnets are configured
     * @throws IllegalArgumentException if the address is not a valid IP literal
     */
    public boolean checkIp(String ip) {
        InetAddress address = InetAddress.ofLiteral(ip);
        if (address instanceof Inet6Address ipv6) {
            for (SubnetUtils6 subnet : allowedIpv6Subnets) {
                if (subnet.getInfo().isInRange(ipv6)) {
                    return true;
                }
            }
            return false;
        }
        for (SubnetUtils subnet : allowedSubnets) {
            if (subnet.getInfo().isInRange(address.getHostAddress())) {
                return true;
            }
        }

        return false;
    }
}
