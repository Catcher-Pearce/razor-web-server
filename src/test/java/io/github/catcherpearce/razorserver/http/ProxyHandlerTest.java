package io.github.catcherpearce.razorserver.http;

import io.github.catcherpearce.razorserver.exception.ProxyConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProxyHandlerTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ", "192.168.1.10", "192.168.1.10/33", "192.168.1.10/-1",
            "256.168.1.10/24", "192.168.1/24", "invalid/24", "192.168.1.10/",
            "192.168.1.10/abc", "::1/129", "2001:db8::", "2001:db8::/-1",
            "2001:db8:::1/64", "localhost/64", "::ffff:192.168.1.10/95"
    })
    void invalidSubnetsExplainCidrAndPreserveCause(String subnet) {
        ProxyConfigurationException exception = assertThrows(
                ProxyConfigurationException.class,
                () -> new ProxyHandler(Arrays.asList(subnet)));

        assertTrue(exception.getMessage().contains("/32"));
        assertTrue(exception.getMessage().contains("/128"));
        assertTrue(exception.getMessage().contains(String.valueOf(subnet)));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void nullSubnetListHasConfigurationException() {
        assertThrows(ProxyConfigurationException.class, () -> new ProxyHandler(null));
    }

    @Test
    void emptySubnetListTrustsNoProxies() {
        assertFalse(new ProxyHandler(List.of()).checkIp("192.168.1.10"));
    }

    @Test
    void singleAddressSubnetTrustsOnlyThatAddress() {
        ProxyHandler handler = new ProxyHandler(List.of("192.168.1.10/32"));

        assertTrue(handler.checkIp("192.168.1.10"));
        assertFalse(handler.checkIp("192.168.1.11"));
    }

    @Test
    void invalidSubnetAfterValidSubnetRejectsConfiguration() {
        ProxyConfigurationException exception = assertThrows(ProxyConfigurationException.class,
                () -> new ProxyHandler(List.of("10.0.0.0/8", "192.168.1.10")));

        assertTrue(exception.getMessage().contains("192.168.1.10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.1.0", "192.168.1.1", "192.168.1.128", "192.168.1.255"})
    void trustsEntireConfiguredSubnetIncludingBoundaries(String address) {
        assertTrue(new ProxyHandler(List.of("192.168.1.0/24")).checkIp(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.0.255", "192.168.2.0", "203.0.113.10"})
    void rejectsAddressesOutsideConfiguredSubnet(String address) {
        assertFalse(new ProxyHandler(List.of("192.168.1.0/24")).checkIp(address));
    }

    @Test
    void trustsAddressesInEitherConfiguredSubnet() {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8", "192.168.1.0/24"));

        assertTrue(handler.checkIp("10.20.30.40"));
        assertTrue(handler.checkIp("192.168.1.25"));
        assertFalse(handler.checkIp("172.16.0.1"));
    }

    @Test
    void slash31TrustsBothAddresses() {
        ProxyHandler handler = new ProxyHandler(List.of("192.168.1.10/31"));

        assertTrue(handler.checkIp("192.168.1.10"));
        assertTrue(handler.checkIp("192.168.1.11"));
        assertFalse(handler.checkIp("192.168.1.12"));
    }

    @Test
    void findsClientBeforeMultipleTrustedProxies() {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8", "192.168.1.0/24"));

        assertEquals("203.0.113.10", handler.findClientIp(
                List.of("203.0.113.10", "10.0.0.5", "192.168.1.20")));
    }

    @Test
    void stopsAtRightmostUntrustedAddressIgnoringSpoofedPrefix() {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8"));

        assertEquals("198.51.100.20", handler.findClientIp(
                List.of("203.0.113.99", "10.0.0.2", "198.51.100.20", "10.0.0.5")));
    }

    @Test
    void returnsLastAddressWhenLastHopIsUntrusted() {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8"));

        assertEquals("198.51.100.20", handler.findClientIp(
                List.of("203.0.113.10", "198.51.100.20")));
    }

    @Test
    void returnsFirstAddressWhenEntireChainIsTrusted() {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8"));

        assertEquals("10.0.0.1", handler.findClientIp(List.of("10.0.0.1", "10.0.0.2", "10.0.0.3")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.1", "203.0.113.10"})
    void returnsOnlyAddressInSingleHopChain(String address) {
        ProxyHandler handler = new ProxyHandler(List.of("10.0.0.0/8"));

        assertEquals(address, handler.findClientIp(List.of(address)));
    }

    @Test
    void noTrustedSubnetsReturnsRightmostAddress() {
        ProxyHandler handler = new ProxyHandler(List.of());

        assertEquals("198.51.100.20", handler.findClientIp(List.of("203.0.113.10", "198.51.100.20")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001:db8::", "2001:db8::1", "2001:0DB8:0000:0000:0000:0000:0000:0001",
            "2001:db8::ffff:ffff:ffff:ffff"})
    void trustsIpv6SubnetIncludingBoundariesAndEquivalentForms(String address) {
        assertTrue(new ProxyHandler(List.of("2001:db8::/64")).checkIp(address));
    }

    @Test
    void rejectsIpv6OutsideSubnetAndDoesNotMixAddressFamilies() {
        ProxyHandler ipv6 = new ProxyHandler(List.of("2001:db8::/64"));
        assertFalse(ipv6.checkIp("2001:db8:0:1::"));
        assertFalse(ipv6.checkIp("192.168.1.10"));
        assertFalse(new ProxyHandler(List.of("192.168.1.0/24")).checkIp("::1"));
    }

    @Test
    void ipv6SingleAddressSubnetTrustsOnlyThatAddress() {
        ProxyHandler handler = new ProxyHandler(List.of("::1/128"));
        assertTrue(handler.checkIp("::1"));
        assertFalse(handler.checkIp("::2"));
    }

    @Test
    void ipv6ZeroPrefixTrustsAllIpv6Addresses() {
        ProxyHandler handler = new ProxyHandler(List.of("::/0"));
        assertTrue(handler.checkIp("::"));
        assertTrue(handler.checkIp("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
        assertFalse(handler.checkIp("192.168.1.10"));
    }

    @Test
    void resolvesMixedFamilyChainStoppingBeforeSpoofedPrefix() {
        ProxyHandler handler = new ProxyHandler(List.of("2001:db8:1::/64", "10.0.0.0/8"));
        assertEquals("2001:db8:2::10", handler.findClientIp(
                List.of("203.0.113.99", "2001:db8:2::10", "10.0.0.1", "2001:db8:1::1")));
        assertEquals("203.0.113.10", handler.findClientIp(List.of("203.0.113.10", "2001:db8:1::1")));
        assertEquals("2001:db8:1::1", handler.findClientIp(List.of("2001:db8:1::1", "10.0.0.1")));
    }

    @Test
    void normalizesMappedIpv6AddressesAndSubnetsToIpv4() {
        ProxyHandler handler = new ProxyHandler(List.of("192.168.1.0/24"));
        assertTrue(handler.checkIp("::ffff:192.168.1.10"));
        assertFalse(handler.checkIp("::ffff:192.168.2.10"));
        ProxyHandler mapped = new ProxyHandler(List.of("::ffff:192.168.1.10/128"));
        assertTrue(mapped.checkIp("192.168.1.10"));
        assertTrue(mapped.checkIp("::ffff:c0a8:10a"));
        assertFalse(mapped.checkIp("192.168.1.11"));
    }
}
