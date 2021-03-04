package com.jemnetworks.portalgun;

import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import org.apache.logging.log4j.LogManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class PortalGunMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("PortalGun");

	public static final PortalGunItem PORTAL_GUN_ITEM = new PortalGunItem();

	public static KeyBinding colorSwitchKey;
	public static KeyBinding portalResetKey;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Registry.register(Registry.ITEM, new Identifier("portal_gun", "portal_gun"), PORTAL_GUN_ITEM);

		colorSwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.portal_gun.change_color",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			"category.portal_gun"
		));
		portalResetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.portal_gun.reset_portals",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"category.portal_gun"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (colorSwitchKey.wasPressed()) {
				onSwitchColor(client);
			}
			while (portalResetKey.wasPressed()) {
				onResetPortals(client);
			}
		});

		System.out.println("Portal Gun mod initialized!");
	}

	public static ItemStack holdingGun(PlayerEntity player) {
		for (ItemStack hand : new ItemStack[] { player.getMainHandStack(), player.getOffHandStack() }) {
			if (hand.getItem() == PORTAL_GUN_ITEM) {
				return hand;
			}
		}
		return null;
	}

	private void onSwitchColor(MinecraftClient client) {
		ItemStack gun = holdingGun(client.player);
		if (gun == null) {
			// client.player.sendMessage(new LiteralText("Not holding portal gun!"), true);
			return;
		}
		PORTAL_GUN_ITEM.switchColor(client.player);
	}

	private void onResetPortals(MinecraftClient client) {
		ItemStack gun = holdingGun(client.player);
		if (gun == null) {
			// client.player.sendMessage(new LiteralText("Not holding portal gun!"), true);
			return;
		}
		PORTAL_GUN_ITEM.resetPortals(client.player);
	}
}
