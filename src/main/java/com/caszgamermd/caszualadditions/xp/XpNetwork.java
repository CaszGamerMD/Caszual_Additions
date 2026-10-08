package com.caszgamermd.caszualadditions.xp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class XpNetwork {
    private XpNetwork() {}

    public static List<XpTankBlockEntity> tanks(ServerLevel level, BlockPos start) {
        var out = new ArrayList<XpTankBlockEntity>();
        var seen = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(start);

        while (!queue.isEmpty() && seen.size() < 4096) {
            BlockPos pos = queue.removeFirst();
            if (!seen.add(pos)) continue;

            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof XpTankBlockEntity tank) {
                out.add(tank);
                for (Direction direction : Direction.values()) {
                    queue.add(pos.relative(direction));
                }
            } else if (pos.equals(start)
                    && (blockEntity instanceof XpChargerBlockEntity
                    || blockEntity instanceof XpDrainBlockEntity
                    || blockEntity instanceof XpShowerBlockEntity)) {
                for (Direction direction : Direction.values()) {
                    queue.add(pos.relative(direction));
                }
            }
        }

        return out;
    }

    public static int stored(ServerLevel level, BlockPos pos) {
        return tanks(level, pos).stream().mapToInt(XpTankBlockEntity::stored).sum();
    }

    public static int space(ServerLevel level, BlockPos pos) {
        return Math.max(0, capacity(level, pos) - stored(level, pos));
    }

    public static int capacity(ServerLevel level, BlockPos pos) {
        return tanks(level, pos).size() * XpTankBlockEntity.CAPACITY;
    }

    public static int extract(ServerLevel level, BlockPos pos, int amount) {
        int left = amount;

        for (XpTankBlockEntity tank : tanks(level, pos)) {
            int take = Math.min(left, tank.stored());
            tank.setStored(tank.stored() - take);
            left -= take;
            if (left == 0) break;
        }

        int moved = amount - left;
        if (moved > 0) refreshVisuals(level, pos);
        return moved;
    }

    public static int insert(ServerLevel level, BlockPos pos, int amount) {
        int left = amount;

        for (XpTankBlockEntity tank : tanks(level, pos)) {
            int put = Math.min(left, XpTankBlockEntity.CAPACITY - tank.stored());
            tank.setStored(tank.stored() + put);
            left -= put;
            if (left == 0) break;
        }

        int moved = amount - left;
        if (moved > 0) refreshVisuals(level, pos);
        return moved;
    }

    /**
     * Makes the connected network render like one physical tank.
     *
     * Storage is pooled across all connected tanks. Visually, XP fills from the
     * lowest connected Y layer upward. Every tank in the same horizontal layer
     * gets the same fill value, so the liquid surface stays level across the
     * entire connected footprint.
     */
    public static void refreshVisuals(ServerLevel level, BlockPos start) {
        List<XpTankBlockEntity> network = tanks(level, start);
        if (network.isEmpty()) return;

        long totalStored = 0L;
        Map<Integer, List<XpTankBlockEntity>> layers = new TreeMap<>();

        for (XpTankBlockEntity tank : network) {
            totalStored += tank.stored();
            layers.computeIfAbsent(tank.getBlockPos().getY(), ignored -> new ArrayList<>()).add(tank);
        }

        long remaining = totalStored;

        for (List<XpTankBlockEntity> layer : layers.values()) {
            long layerCapacity = (long) layer.size() * XpTankBlockEntity.CAPACITY;
            long layerStored = Math.min(remaining, layerCapacity);

            int fill;
            if (layerStored <= 0) {
                fill = 0;
            } else {
                fill = (int) Math.min(
                        10L,
                        (layerStored * 10L + layerCapacity - 1L) / layerCapacity
                );
            }

            for (XpTankBlockEntity tank : layer) {
                tank.setVisualFill(fill);
            }

            remaining = Math.max(0L, remaining - layerCapacity);
        }
    }
}
