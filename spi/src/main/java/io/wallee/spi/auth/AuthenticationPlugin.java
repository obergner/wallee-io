package io.wallee.spi.auth;

import java.security.Principal;
import java.util.Optional;

/**
 *
 */
public interface AuthenticationPlugin {

    Optional<Principal> authenticate(Credentials clientCredentials);
}
