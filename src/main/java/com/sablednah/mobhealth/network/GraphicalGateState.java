package com.sablednah.mobhealth.network;

/**
 * Client-side flag: may this client draw graphical floating bars right now?
 *
 * <p>Set by the {@link GraphicalGatePayload} the server sends (effective value = server's
 * {@code graphical} config AND the player's {@code mobhealth.see} permission AND not muted). Defaults
 * to {@code true} so that on vanilla / non-MobHealth servers (which never send the packet) the
 * client's own config still decides.
 *
 * <p>Deliberately free of Minecraft imports so the network handler that writes it can be referenced
 * on a dedicated server without pulling in client classes.
 */
public final class GraphicalGateState {

    private GraphicalGateState() {}

    public static volatile boolean serverAllowsGraphical = true;
}
