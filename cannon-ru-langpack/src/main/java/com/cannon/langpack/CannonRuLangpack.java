package com.cannon.langpack;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CannonRuLangpack.MOD_ID)
public class CannonRuLangpack {
    public static final String MOD_ID = "cannon_ru_langpack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CannonRuLangpack() {
        LOGGER.info("Cannon Russian Langpack loaded.");
    }
}
