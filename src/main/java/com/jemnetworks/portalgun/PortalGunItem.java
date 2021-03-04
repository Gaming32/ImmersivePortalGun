package com.jemnetworks.portalgun;

import com.qouteall.immersive_portals.network.McRemoteProcedureCall;

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
    }

    public PortalGunItem() {
        super(new FabricItemSettings()
            .group(ItemGroup.TOOLS)
            .maxCount(1)
        );
    }

    private void newGun(ItemStack stack, World world, PlayerEntity player) {
        CompoundTag dataTag = stack.getOrCreateTag();
        dataTag.putBoolean("color", false);
        dataTag.putByte("state", (byte)0);
        System.out.println("New portal gun!");
    }

    private void putMarker(CompoundTag tag, String key, BlockHitResult info, Direction playerDirection) {
        CompoundTag newTag = new CompoundTag();
        newTag.putInt("x", info.getBlockPos().getX());
        newTag.putInt("y", info.getBlockPos().getY());
        newTag.putInt("z", info.getBlockPos().getZ());
        newTag.putInt("blockDirection",  info.getSide().getId());
        newTag.putInt("playerDirection", playerDirection.getId());
        tag.put(key, newTag);
    }

    private Marker getMarker(CompoundTag tag, String key) {
        CompoundTag vector = tag.getCompound(key);
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

    private void createPortal(CompoundTag gun, BlockHitResult hit, Direction playerDirection, Marker marker) {

    }

    public void inShootGun(PlayerEntity player, CompoundTag gun, BlockHitResult hit) {
        Direction playerDirection = player.getHorizontalFacing();
        boolean color = gun.getBoolean("color");
        switch (gun.getByte("state")) {
            case (byte)0:
                gun.putBoolean("markerColor", color);
                putMarker(gun, "marker", hit, playerDirection);
                gun.putByte("state", (byte)1);
                break;
            case (byte)1:
                boolean markerColor = gun.getBoolean("markerColor");
                if (markerColor == color) {
                    gun.putBoolean("markerColor", color);
                    putMarker(gun, "marker", hit, playerDirection);
                }
                else {
                    createPortal(gun, hit, playerDirection, getMarker(gun, "marker"));
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

    private void shootGun(CompoundTag gun, BlockHitResult hit, PlayerEntity player) {
        
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack thisGun = playerEntity.getStackInHand(hand);
        if (!thisGun.hasTag())
            newGun(thisGun, world, playerEntity);

        HitResult hit = client.crosshairTarget;
        if (hit.getType() != Type.BLOCK) { // We didn't actually hit a block
            return TypedActionResult.fail(thisGun);
        }

        BlockHitResult blockHit = (BlockHitResult)hit;
        // System.out.println(blockHit.getBlockPos());
        // System.out.println(blockHit.getSide());
        // System.out.println(playerEntity.getHorizontalFacing());
        shootGun(thisGun.getTag(), blockHit, playerEntity);

        return TypedActionResult.success(thisGun);
    }
}
