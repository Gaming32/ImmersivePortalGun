package com.jemnetworks.portalgun;

import com.qouteall.immersive_portals.network.McRemoteProcedureCall;
import com.qouteall.immersive_portals.portal.Portal;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PortalGunItem extends Item {
    private class Marker {
        BlockPos position;
        Direction side;
        Direction playerDirection;

        public Marker(BlockPos position, Direction side, Direction playerDirection) {
            this.position = position;
            this.side = side;
            this.playerDirection = playerDirection;
        }

        public String toString() {
            return "{Marker"
                 + " position=" + this.position
                 + " side=" + this.side
                 + " playerDirection=" + this.playerDirection
                 + "}";
        }
    }

    public PortalGunItem() {
        super(new FabricItemSettings()
            .group(ItemGroup.TOOLS)
            .maxCount(1)
        );
    }

    private void newGun(ItemStack stack, PlayerEntity player) {
        CompoundTag dataTag = stack.getOrCreateTag();
        dataTag.putBoolean("color", false);
        dataTag.putByte("state", (byte)0);
        System.out.println("New portal gun!");
    }

    private void putMarkerDirect(CompoundTag tag, BlockHitResult info, Direction playerDirection) {
        tag.putInt("x", info.getBlockPos().getX());
        tag.putInt("y", info.getBlockPos().getY());
        tag.putInt("z", info.getBlockPos().getZ());
        tag.putInt("blockDirection",  info.getSide().getId());
        tag.putInt("playerDirection", playerDirection.getId());
    }

    private void putMarkerDirect(CompoundTag tag, Marker marker) {
        tag.putInt("x", marker.position.getX());
        tag.putInt("y", marker.position.getY());
        tag.putInt("z", marker.position.getZ());
        tag.putInt("blockDirection",  marker.side.getId());
        tag.putInt("playerDirection", marker.playerDirection.getId());
    }

    private void putMarker(CompoundTag tag, String key, Marker marker) {
        CompoundTag newTag = new CompoundTag();
        putMarkerDirect(newTag, marker);
        tag.put(key, newTag);
    }

    private void putMarker(CompoundTag tag, String key, BlockHitResult info, Direction playerDirection) {
        CompoundTag newTag = new CompoundTag();
        putMarkerDirect(newTag, info, playerDirection);
        tag.put(key, newTag);
    }

    public Marker getMarkerDirect(CompoundTag vector) {
        return new Marker(
            new BlockPos(
                vector.getInt("x"),
                vector.getInt("y"),
                vector.getInt("z")
            ),
            Direction.byId(vector.getInt("blockDirection")),
            Direction.byId(vector.getInt("playerDirection"))
        );
    }

    private Marker getMarker(CompoundTag tag, String key) {
        CompoundTag vector = tag.getCompound(key);
        return getMarkerDirect(vector);
    }

    private void inSwitchColor(CompoundTag gun, boolean newColor) {
        gun.putBoolean("color", newColor);
    }

    private TranslatableText getColorText(boolean color) {
        if (color) {
            return new TranslatableText("color.minecraft.blue");
        }
        return new TranslatableText("color.minecraft.orange");
    }

    private void sendColorChangeMesssage(ClientPlayerEntity player, boolean color) {
        TranslatableText baseText = new TranslatableText("item.portal_gun.portal_gun.changed_color");
        player.sendMessage(baseText.append(getColorText(color)), true);
    }

    @SuppressWarnings("resource")
    public void sendColorChangeMesssage(boolean color) {
        sendColorChangeMesssage(MinecraftClient.getInstance().player, color);
    }

    public void switchColor(ClientPlayerEntity player) {
        McRemoteProcedureCall.tellServerToInvoke(
            "com.jemnetworks.portalgun.PortalGunItemRemoteCallable.switchColor"
        );
    }

    public void switchColor(ServerPlayerEntity player, CompoundTag gun) {
        boolean origColor = gun.getBoolean("color");
        inSwitchColor(gun, !origColor);
        McRemoteProcedureCall.tellClientToInvoke(
            player,
            "com.jemnetworks.portalgun.PortalGunItemRemoteCallable.sendColorChangeMesssage",
            !origColor
        );
    }

    public void inResetPortals(ServerPlayerEntity player, CompoundTag gun) {
        gun.putByte("state", (byte)0);
        gun.remove("markerColor");
        gun.remove("marker");
        gun.remove("portal0");
        gun.remove("portal1");
    }

    public void resetPortals(ClientPlayerEntity player) {
        McRemoteProcedureCall.tellServerToInvoke(
            "com.jemnetworks.portalgun.PortalGunItemRemoteCallable.resetPortals"
        );
        player.sendMessage(new TranslatableText("item.portal_gun.portal_gun.portals_reset"), true);
    }

    // private Vec3d getOffset(BlockPos position, Direction direction) {
    //     switch (direction) {
    //         case UP:
    //             return new Vec3d(0.5, 1, 0.5);
    //         case DOWN:
    //             return new Vec3d(0.5, 0, 0.5);
    //         case NORTH:
    //             return new Vec3d(0.5, 0.5, 1);
    //         case SOUTH:
    //             return new Vec3d(0.5, 0.5, 0);
    //         case EAST:
    //             return new Vec3d()
    //     }
    // }

    // private Vec3d[] getRotation

    private Portal createPortal(CompoundTag gun, World world, Marker portalPos) {
        if (portalPos.side == Direction.DOWN || portalPos.side == Direction.UP) return null;
        Portal portal = Portal.entityType.create(world);
        Vec3d unitVec = new Vec3d(portalPos.side.getUnitVector());
        portal.setOriginPos(Vec3d.ofCenter(portalPos.position).add(unitVec.multiply(0.5001).subtract(new Vec3d(0, 0.5, 0))));
        portal.setDestinationDimension(world.getRegistryKey());
        portal.setOrientationAndSize(unitVec.rotateY((float)(0.5*Math.PI)), new Vec3d(0, 1, 0), 1, 2);
        return portal;
    }

    private float getAngle(Direction dir) {
        switch (dir) {
            case NORTH:
                return 90;
            case SOUTH:
                return -90;
            case EAST:
                return 90;
            case WEST:
                return -90;
            default:
                return 0;
        }
    }

    private Direction portalFacing(Vec3d axisW) {
        return Direction.fromVector((int)axisW.x, 0, (int)axisW.z);
    }

    private void updatePortal(Portal portal, Portal other) {
        portal.setDestinationDimension(other.getOriginDim());
        portal.setDestination(other.getOriginPos());
        // System.out.println(portal.axisH.());
        Direction portalFacing = portalFacing(portal.axisW);
        Direction otherFacing = portalFacing(other.axisW);
        float angle = getAngle(portalFacing) + getAngle(otherFacing);
        if (portalFacing != otherFacing && portalFacing != otherFacing.getOpposite()) {
            if ((portalFacing == Direction.SOUTH && otherFacing == Direction.WEST) ||
                (portalFacing == Direction.NORTH && otherFacing == Direction.EAST))
                angle += portalFacing.getHorizontal() * 90 - otherFacing.getHorizontal() * 90;
            else
                angle += otherFacing.getHorizontal() * 90 - portalFacing.getHorizontal() * 90;
        }
        
        portal.setRotationTransformation(new Quaternion(0, angle, 0, true));
        portal.world.spawnEntity(portal);
    }

    private void createPortals(CompoundTag gun, World world, Marker portal1Pos, Marker portal2Pos) {
        System.out.println("New portal! From:" + portal1Pos + " To:" + portal2Pos);
        Portal portal1 = createPortal(gun, world, portal1Pos);
        if (portal1 == null) return;
        Portal portal2 = createPortal(gun, world, portal2Pos);
        if (portal2 == null) return;
        updatePortal(portal1, portal2);
        updatePortal(portal2, portal1);
        // if (portal1Pos.side == Direction.DOWN || portal1Pos.side == Direction.UP) return;
        // if (portal2Pos.side == Direction.DOWN || portal2Pos.side == Direction.UP) return;
        // Portal portal1 = Portal.entityType.create(world);
        // Vec3d unitVec = new Vec3d(portal1Pos.side.getUnitVector());
        // portal1.setOriginPos(Vec3d.ofCenter(portal1Pos.position).add(unitVec.multiply(0.5001).subtract(new Vec3d(0, 0.5, 0))));
        // portal1.setDestinationDimension(World.NETHER);
        // portal1.setDestination(Vec3d.ofCenter(portal1Pos.position).multiply(1/8));
        // portal1.setOrientationAndSize(unitVec.rotateY((float)(0.5*Math.PI)), new Vec3d(0, 1, 0), 1, 2);
        // portal1.world.spawnEntity(portal1);
    }

    public void inShootGun(ServerPlayerEntity player, Marker hit) {
        ItemStack gunItem = PortalGunMod.holdingGun(player);
        if (gunItem == null) {
            PortalGunMod.LOGGER.error("Player used non-existent portal gun!");
            return;
        }
        if (!gunItem.hasTag())
            newGun(gunItem, player);
        CompoundTag gun = gunItem.getTag();
        boolean color = gun.getBoolean("color");
        switch (gun.getByte("state")) {
            case (byte)0:
                gun.putBoolean("markerColor", color);
                putMarker(gun, "marker", hit);
                gun.putByte("state", (byte)1);
                break;
            case (byte)1:
                boolean markerColor = gun.getBoolean("markerColor");
                if (markerColor == color) {
                    gun.putBoolean("markerColor", color);
                    putMarker(gun, "marker", hit);
                }
                else {
                    createPortals(gun, player.world, hit, getMarker(gun, "marker"));
                    gun.putByte("state", (byte)2);
                }
                break;
            case (byte)2:
                break;
            default:
                PortalGunMod.LOGGER.warn("Invalid gun.state {}", gun.getByte("state"));
                gun.putByte("state", (byte)0);
        }
        switchColor((ServerPlayerEntity)player, gun);
    }

    private void shootGun(BlockHitResult hit, PlayerEntity player) {
        CompoundTag hitTag = new CompoundTag();
        putMarkerDirect(hitTag, hit, player.getHorizontalFacing());
        McRemoteProcedureCall.tellServerToInvoke(
            "com.jemnetworks.portalgun.PortalGunItemRemoteCallable.shootGun",
            hitTag
        );
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        if (playerEntity.getClass() != ClientPlayerEntity.class)
            return TypedActionResult.fail(playerEntity.getStackInHand(hand));

        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack thisGun = playerEntity.getStackInHand(hand);

        HitResult hit = client.crosshairTarget;
        if (hit.getType() != Type.BLOCK) { // We didn't actually hit a block
            return TypedActionResult.fail(thisGun);
        }

        BlockHitResult blockHit = (BlockHitResult)hit;
        // System.out.println(blockHit.getBlockPos());
        // System.out.println(blockHit.getSide());
        // System.out.println(playerEntity.getHorizontalFacing());
        shootGun(blockHit, playerEntity);

        return TypedActionResult.success(thisGun);
    }
}
