package com.example.addon.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

import static meteordevelopment.meteorclient.MeteorClient.mc;
public class Logger {
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
}
