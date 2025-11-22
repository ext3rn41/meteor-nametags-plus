package dev.ext3rn41.meteornametagsplus;

import dev.ext3rn41.meteornametagsplus.modules.NametagsPlus;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NametagPlus extends MeteorAddon {
    public static final Logger logger = LoggerFactory.getLogger(NametagPlus.class);

    @Override
    public void onInitialize() {
        logger.info("Initializing Nametag+");

        Modules.get().add(new NametagsPlus());
    }

    @Override
    public void onRegisterCategories() {
        // 카테고리 따로안만들거임
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("ext3rn41", "meteor-nametags-plus");
    }

    @Override
    public String getPackage() {
        return "dev.ext3rn41.meteornametagsplus";
    }
}
