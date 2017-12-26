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

package io.wallee.shared.plugin.auth;

import java.net.InetSocketAddress;
import java.security.Principal;

/**
 * A {@link Principal} implementation that represents an authenticated remote MQTT client.
 */
public class ClientPrincipal implements Principal {

    private final String name;

    private final InetSocketAddress clientAddress;

    public ClientPrincipal(final String name, final InetSocketAddress clientAddress) {
        if (clientAddress == null) {
            throw new IllegalArgumentException("Argument 'clientAddress' must not be null");
        }
        this.name = name;
        this.clientAddress = clientAddress;
    }

    @Override
    public String getName() {
        return null;
    }

    public InetSocketAddress getClientAddress() {
        return clientAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ClientPrincipal that = (ClientPrincipal) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return clientAddress.equals(that.clientAddress);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + clientAddress.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClientPrincipal[" +
                "name:'" + name + '\'' +
                "|clientAddress:" + clientAddress +
                ']';
    }
}
