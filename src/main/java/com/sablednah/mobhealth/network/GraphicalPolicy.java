package com.sablednah.mobhealth.network;

/**
 * The graphical-bar policy the server pushes to a client: whether the client may draw bars, plus
 * per-option overrides. A {@code null} override means "let the client's own config decide"; a
 * non-null value is enforced by the server.
 *
 * <p>Plain data (no Minecraft imports) so it can live in {@link GraphicalGateState} and be touched
 * from either side.
 */
public record GraphicalPolicy(
        boolean allowed,
        Boolean requireLineOfSight,
        Boolean showText,
        Boolean showBackground,
        Boolean showPlayers,
        Boolean onlyWhenDamaged,
        Double verticalOffset,
        Double maxDistance,
        Integer barWidth,
        Integer barHeight,
        Double scale,
        Boolean scaleWithDistance,
        Boolean fadeWithDistance) {

    /** No enforcement: allowed, and every option left to the client. Used on vanilla servers / logout. */
    public static final GraphicalPolicy DEFAULT = new GraphicalPolicy(
            true, null, null, null, null, null, null, null, null, null, null, null, null);

    // Resolve helpers: server override if present, otherwise the supplied client value.
    public boolean requireLineOfSight(boolean clientValue) {
        return requireLineOfSight != null ? requireLineOfSight : clientValue;
    }

    public boolean showText(boolean clientValue) {
        return showText != null ? showText : clientValue;
    }

    public boolean showBackground(boolean clientValue) {
        return showBackground != null ? showBackground : clientValue;
    }

    public boolean showPlayers(boolean clientValue) {
        return showPlayers != null ? showPlayers : clientValue;
    }

    public boolean onlyWhenDamaged(boolean clientValue) {
        return onlyWhenDamaged != null ? onlyWhenDamaged : clientValue;
    }

    public double verticalOffset(double clientValue) {
        return verticalOffset != null ? verticalOffset : clientValue;
    }

    public double maxDistance(double clientValue) {
        return maxDistance != null ? maxDistance : clientValue;
    }

    public int barWidth(int clientValue) {
        return barWidth != null ? barWidth : clientValue;
    }

    public int barHeight(int clientValue) {
        return barHeight != null ? barHeight : clientValue;
    }

    public double scale(double clientValue) {
        return scale != null ? scale : clientValue;
    }

    public boolean scaleWithDistance(boolean clientValue) {
        return scaleWithDistance != null ? scaleWithDistance : clientValue;
    }

    public boolean fadeWithDistance(boolean clientValue) {
        return fadeWithDistance != null ? fadeWithDistance : clientValue;
    }
}
