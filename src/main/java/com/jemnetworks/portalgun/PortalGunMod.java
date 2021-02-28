package com.jemnetworks.portalgun;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class PortalGunMod implements ModInitializer {
	public static final PortalGunItem PORTAL_GUN_ITEM = new PortalGunItem();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Registry.register(Registry.ITEM, new Identifier("portal_gun", "portal_gun"), PORTAL_GUN_ITEM);

		System.out.println("Portal Gun mod initialized!");
	}
}
