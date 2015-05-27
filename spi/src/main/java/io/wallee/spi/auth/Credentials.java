package io.wallee.spi.auth;

import java.net.InetSocketAddress;

/**
 *
 */
public final class Credentials {

    private final String username;

    private final String password;

    private final InetSocketAddress clientAddress;

    public Credentials(final String username, final String password, final InetSocketAddress clientAddress) {
        if (clientAddress == null) {
            throw new IllegalArgumentException("Argument 'clientAddress' must not be null");
        }
        this.username = username;
        this.password = password;
        this.clientAddress = clientAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public InetSocketAddress getClientAddress() {
        return clientAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Credentials that = (Credentials) o;

        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        return clientAddress.equals(that.clientAddress);

    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + clientAddress.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Credentials[" +
                "username:'" + username + '\'' +
                "|clientAddress:" + clientAddress +
                "|password:'" + "XXXXXXXXXX" + '\'' +
                ']';
    }
}
