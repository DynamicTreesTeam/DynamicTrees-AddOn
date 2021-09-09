package com.example.dtaddon;

import com.ferreusveritas.dynamictrees.api.registry.RegistryHandler;
import net.minecraftforge.fml.common.Mod;

/**
 * The main mod class.
 */
@Mod(DTAddOn.MOD_ID)
public final class DTAddOn {

    /** The mod's ID. This is replaced automatically when compiled by the value set in {@code gradle.properties}. */
    public static final String MOD_ID = "@MOD_ID@";

    public DTAddOn() {
        // Required for DT to register blocks and items automatically at the right time.
        RegistryHandler.setup(MOD_ID);
    }

}
