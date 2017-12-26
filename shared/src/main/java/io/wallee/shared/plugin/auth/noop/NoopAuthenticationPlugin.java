/*
 * Copyright 2015 Olaf Bergner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
