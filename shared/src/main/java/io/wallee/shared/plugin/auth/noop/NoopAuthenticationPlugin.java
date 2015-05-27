package io.wallee.shared.plugin.auth.noop;

import io.wallee.shared.plugin.auth.ClientPrincipal;
import io.wallee.spi.auth.AuthenticationPlugin;
import io.wallee.spi.auth.Credentials;

import java.security.Principal;
import java.util.Optional;

/**
 * An {@link AuthenticationPlugin} that simply accepts <strong>all</strong> authentication requests.
 */
public class NoopAuthenticationPlugin implements AuthenticationPlugin {

    @Override
    public Optional<Principal> authenticate(final Credentials clientCredentials) {
        return Optional.of(new ClientPrincipal(clientCredentials.getUsername(), clientCredentials.getClientAddress()));
    }
}
