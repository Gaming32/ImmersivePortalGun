package com.jemnetworks.portalgun;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class PortalGunItem extends Item {
    public PortalGunItem() {
        super(new FabricItemSettings()
            .group(ItemGroup.MISC)
            .maxCount(1)
        );
    }
}
