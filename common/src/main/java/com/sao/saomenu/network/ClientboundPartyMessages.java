package com.sao.saomenu.network;

import java.util.List;
import java.util.Objects;

/** Client-only delivery is installed at client startup; packet classes remain dedicated-server safe. */
public final class ClientboundPartyMessages {
    public interface Receiver {
        void invite(String inviter);

        void team(String title, List<String> members);
    }

    private static Receiver receiver;

    private ClientboundPartyMessages() {
    }

    public static void install(Receiver clientReceiver) {
        if (receiver != null) {
            throw new IllegalStateException("Client party receiver is already installed");
        }
        receiver = Objects.requireNonNull(clientReceiver, "clientReceiver");
    }

    private static Receiver receiver() {
        if (receiver == null) {
            throw new IllegalStateException("Client party receiver has not been initialized");
        }
        return receiver;
    }

    public static void invite(String inviter) {
        receiver().invite(inviter);
    }

    public static void team(String title, List<String> members) {
        receiver().team(title, members);
    }
}
