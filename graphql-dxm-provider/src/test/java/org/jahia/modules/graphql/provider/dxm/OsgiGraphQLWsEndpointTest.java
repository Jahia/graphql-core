package org.jahia.modules.graphql.provider.dxm;

import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.jahia.modules.graphql.provider.dxm.OsgiGraphQLWsEndpoint.isAllowedOrigin;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OsgiGraphQLWsEndpointTest {

    private static final URI LOCAL = URI.create("ws://localhost:8080/modules/graphqlws");
    private static final URI PUBLIC = URI.create("wss://www.example.com/modules/graphqlws");
    private static final Set<String> NONE = Collections.emptySet();

    @Test
    public void acceptsTheOriginTheRequestIsAddressedTo() {
        assertTrue(isAllowedOrigin("http://localhost:8080", LOCAL, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com", PUBLIC, NONE));
    }

    @Test
    public void comparesHostsIgnoringCaseAndPortsWithTheirDefault() {
        assertTrue(isAllowedOrigin("http://LocalHost:8080", LOCAL, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com:443", PUBLIC, NONE));
        assertTrue(isAllowedOrigin("https://www.example.com", URI.create("wss://www.example.com:443/modules/graphqlws"), NONE));
    }

    @Test
    public void refusesAnotherPortSchemeOrHost() {
        assertFalse(isAllowedOrigin("http://localhost:9999", LOCAL, NONE));
        assertFalse(isAllowedOrigin("http://localhost", LOCAL, NONE));
        assertFalse(isAllowedOrigin("https://localhost:8080", LOCAL, NONE));
        assertFalse(isAllowedOrigin("http://www.example.com", PUBLIC, NONE));
        assertFalse(isAllowedOrigin("https://other.example.com", PUBLIC, NONE));
        assertFalse(isAllowedOrigin("https://www.example.com.other.net", PUBLIC, NONE));
    }

    @Test
    public void refusesAValueThatIsNotASerializedOrigin() {
        for (String origin : asList(null, "", "null", "file://", "localhost:8080", "ws://localhost:8080",
                "http://localhost:8080/", "http://user@localhost:8080", "http://localhost:8080?a=b", "http://local host:8080")) {
            assertFalse(String.valueOf(origin), isAllowedOrigin(origin, LOCAL, NONE));
        }
    }

    @Test
    public void acceptsAConfiguredOrigin() {
        Set<String> configured = new HashSet<>(asList("https://front.example.com", "http://localhost:3000"));
        assertTrue(isAllowedOrigin("https://front.example.com", PUBLIC, configured));
        assertTrue(isAllowedOrigin("http://localhost:3000", LOCAL, configured));
        assertFalse(isAllowedOrigin("https://front.example.com:8443", PUBLIC, configured));
        assertFalse(isAllowedOrigin("https://FRONT.example.com", PUBLIC, configured));
    }

    @Test
    public void refusesAConfiguredValueThatIsNotASerializedOrigin() {
        assertFalse(isAllowedOrigin("null", LOCAL, Collections.singleton("null")));
        assertFalse(isAllowedOrigin("*", LOCAL, Collections.singleton("*")));
    }

    @Test
    public void refusesAnUnconfiguredOriginWhenTheRequestAddressIsUnknown() {
        assertFalse(isAllowedOrigin("http://localhost:8080", null, NONE));
        assertFalse(isAllowedOrigin("http://localhost:8080", URI.create("/modules/graphqlws"), NONE));
        assertTrue(isAllowedOrigin("http://localhost:8080", null, Collections.singleton("http://localhost:8080")));
    }
}
