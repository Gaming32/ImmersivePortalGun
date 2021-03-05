package com.jemnetworks.portalgun;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;

public class PortalGunItemRemoteCallable {
    public static void switchColor(ServerPlayerEntity player) {
        PortalGunMod.PORTAL_GUN_ITEM.switchColor(player, PortalGunMod.holdingGun(player).getTag());
    }

    public static void sendColorChangeMesssage(boolean color) {
        PortalGunMod.PORTAL_GUN_ITEM.sendColorChangeMesssage(color);
    }

    public static void resetPortals(ServerPlayerEntity player) {
        PortalGunMod.PORTAL_GUN_ITEM.inResetPortals(player, PortalGunMod.holdingGun(player).getTag());
    }

    public static void shootGun(ServerPlayerEntity player, CompoundTag hit) {
        PortalGunMod.PORTAL_GUN_ITEM.inShootGun(player, PortalGunMod.PORTAL_GUN_ITEM.getMarkerDirect(hit));
    }
}
