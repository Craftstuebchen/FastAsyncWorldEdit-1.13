package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.object.mask.IdMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * A pickaxe mode that recursively finds adjacent blocks within range of
 * an initial block and of the same type.
 */
public class RecursivePickaxe implements BlockTool {

    private double range;

    public RecursivePickaxe(double range) {
        this.range = range;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.superpickaxe.recursive");
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session, com.sk89q.worldedit.util.Location clicked) {
        World world = (World) clicked.getExtent();

        BlockVector3 origin = clicked.toVector().toBlockPoint();
        BlockType initialType = world.getBlock(origin).getBlockType();

        if (initialType.getMaterial().isAir()) {
            return true;
        }

        if (initialType == BlockTypes.BEDROCK && !player.canDestroyBedrock()) {
            return true;
        }

        try (EditSession editSession = session.createEditSession(player)) {
            editSession.getSurvivalExtent().setToolUse(config.superPickaxeManyDrop);

            try {
                recurse(server, editSession, world, clicked.toVector().toBlockPoint(),
                        clicked.toVector().toBlockPoint(), range, initialType, new HashSet<>());
            } catch (MaxChangedBlocksException e) {
                player.printError("Max blocks change limit reached.");
            } finally {
                session.remember(editSession);
            }
        }

        return true;
    }

    private static void recurse(Platform server, EditSession editSession, World world, BlockVector3 pos,
            BlockVector3 origin, double size, BlockType initialType, Set<BlockVector3> visited) throws MaxChangedBlocksException {

        final double distanceSq = origin.distanceSq(pos);
        if (distanceSq > size*size || visited.contains(pos)) {
            return;
        }

        visited.add(pos);

        if (editSession.getBlock(pos).getBlockType() != initialType) {
            return;
        }

        world.queueBlockBreakEffect(server, pos, initialType, distanceSq);

        editSession.setBlock(pos, BlockTypes.AIR.getDefaultState());

        recurse(server, editSession, world, pos.add(1, 0, 0),
                origin, size, initialType, visited);
        recurse(server, editSession, world, pos.add(-1, 0, 0),
                origin, size, initialType, visited);
        recurse(server, editSession, world, pos.add(0, 0, 1),
                origin, size, initialType, visited);
        recurse(server, editSession, world, pos.add(0, 0, -1),
                origin, size, initialType, visited);
        recurse(server, editSession, world, pos.add(0, 1, 0),
                origin, size, initialType, visited);
        recurse(server, editSession, world, pos.add(0, -1, 0),
                origin, size, initialType, visited);
    }

}
