package com.example.addon;

import com.example.addon.Utils.RendererUtils;
import com.example.addon.commands.CommandExample;
import com.example.addon.hud.HudExample;
import com.example.addon.modules.ArrowBlock;
import com.example.addon.modules.AutoEXP;
import com.example.addon.modules.ESP;
import com.example.addon.modules.JumpEffect;
import com.example.addon.modules.KillAuraCrit;
import com.example.addon.modules.dupe;
import com.example.addon.modules.ChatyBot;
import com.example.addon.modules.NoFall;
import com.example.addon.modules.Trajectories;
import com.example.addon.modules.VeinMiner;
import com.example.addon.modules.autoFarm;
import com.example.addon.modules.autoShield;
import com.example.addon.modules.hunt;
import com.example.addon.modules.autoMove;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;


import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Example");
    public static final HudGroup HUD_GROUP = new HudGroup("Example");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules
        Modules.get().add(new ChatyBot());
        Modules.get().add(new dupe());
        Modules.get().add(new NoFall());
        Modules.get().add(new autoFarm());
        Modules.get().add(new VeinMiner());
        Modules.get().add(new RendererUtils());
        Modules.get().add(new ArrowBlock());
        Modules.get().add(new KillAuraCrit());
        Modules.get().add(new Trajectories());
        Modules.get().add(new AutoEXP());
        Modules.get().add(new JumpEffect());
        Modules.get().add(new hunt());
        Modules.get().add(new autoMove());

        // Modules.get().add(new ESP());


        // Commands
        Commands.get().add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
