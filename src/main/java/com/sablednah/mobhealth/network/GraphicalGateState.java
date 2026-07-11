package com.sablednah.mobhealth.network;

/**
 * Client-side holder for the current graphical-bar {@link GraphicalPolicy} sent by the server.
 *
 * <p>Defaults to {@link GraphicalPolicy#DEFAULT} (allowed, no overrides) so that on vanilla /
 * non-MobHealth servers (which never send the packet) the client's own config decides. Reset back
 * to the default when leaving a server.
 *
 * <p>Deliberately free of Minecraft imports so the network handler that writes it can be referenced
 * on a dedicated server without pulling in client classes.
 */
public final class GraphicalGateState {

    private GraphicalGateState() {}

    public static volatile GraphicalPolicy policy = GraphicalPolicy.DEFAULT;
}
