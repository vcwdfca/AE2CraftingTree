package com.vcwdfca.ae2ct;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AE2ct.MODID)
public class AE2ct {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "ae2ct";

    public AE2ct() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CONFIG);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

}
