## Configuring trusted proxies

If Razor runs behind a reverse proxy, configure the IP addresses or subnets of the proxies you trust. Call `configureProxy()` before starting the server:

```java
import java.util.List;

RazorServer server = new RazorServer(3000);

server.configureProxy(List.of(
        "192.168.1.10/32",
        "10.0.0.0/24",
        "2001:db8::/64",
        "::1/128"
));

server.start();
```

Each entry must use CIDR notation:

| Example | Addresses trusted |
| --- | --- |
| `192.168.1.10/32` | One IPv4 address |
| `10.0.0.0/24` | IPv4 addresses from `10.0.0.0` through `10.0.0.255` |
| `2001:db8::/64` | An IPv6 subnet |
| `::1/128` | The IPv6 loopback address |

For a single address, append `/32` for IPv4 or `/128` for IPv6. Invalid configuration throws `ProxyConfigurationException` during setup. An empty list trusts no proxies.

### How Razor determines the client IP

Razor first reads the IP address of the peer directly connected to the server. It uses the `X-Forwarded-For` header only when that peer belongs to a configured trusted subnet.

For a trusted peer, Razor scans the forwarded addresses from right to left, skipping trusted proxies. The first untrusted address becomes the client IP. If every address is trusted, Razor uses the leftmost address.

For example:

```text
Trusted subnet: 10.0.0.0/24
Connected peer: 10.0.0.10
X-Forwarded-For: 203.0.113.25, 10.0.0.20

Resolved client IP: 203.0.113.25
```

When proxy handling is unconfigured, the peer is untrusted, or the header is absent, the connected peer's IP remains the client IP.

IPv4 and IPv6 addresses can appear in the same chain. IPv4-mapped IPv6 addresses, such as `::ffff:192.168.1.10`, are matched using IPv4 trust rules.

### Accessing the addresses

Register this route before calling `start()` to expose both addresses:

```java
server.get("/client", request -> Response.ok(Map.of(
        "remoteIp", request.remoteId(),
        "clientIp", request.clientId()
)));
```

Import `java.util.Map` for this example. `remoteId()` contains the connected peer's IP; `clientId()` contains the resolved client IP.

Configure only proxies you control or trust to maintain the forwarded header correctly. These subnets determine which proxies Razor trusts; they do not restrict which clients may connect.

Malformed forwarded headers can currently cause request processing to fail. Configure your proxy to send valid, comma-separated IP addresses.
