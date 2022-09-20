package com.example.addon.Utils;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
public class Logger {
    public static String _TickLog;
    public static void Log(String Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
    }
    public static ActionResult Log(ActionResult Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
        return Log;
    }
    public static Boolean Log(Boolean Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
        return Log;

    }

    public static void TickLog(String log){
        _TickLog = log;
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        Log("heeh");
        if (_TickLog != null) Log(_TickLog);
        _TickLog = null;
    }
}
