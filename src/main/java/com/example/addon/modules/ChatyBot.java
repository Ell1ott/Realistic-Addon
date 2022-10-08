package com.example.addon.modules;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.addon.Addon;
import com.example.addon.Utils.Logger;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import meteordevelopment.orbit.EventHandler;

public class ChatyBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(60)
        .range(1, 200)
        .sliderRange(1, 200)
        .build()
    );

    private final Setting<Boolean> blockchattting = sgGeneral.add(new BoolSetting.Builder()
        .name("block-chattting")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(false)
        .build()
    );


    public ChatyBot() {
        super(Addon.CATEGORY, "Chat-Bot", "answers math questions");

    }

    int timer = 0;
    Pattern MathPattern = Pattern.compile("hvad er ([0-9]+ ?[+\\-/*]+ ?[0-9]+)", Pattern.CASE_INSENSITIVE);
    Pattern WordPattern = Pattern.compile("gæt ordet: \"(.+)\"", Pattern.CASE_INSENSITIVE);


    String answer = null;

    @Override
    public void onActivate() {
        timer = 0;







;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        timer = timer + 1;

        if(timer > delay.get() && answer != null){
            mc.player.sendChatMessage(answer, null);
            answer = null;
        }

    }

    @EventHandler
    private void onMessageRecieve(ReceiveMessageEvent event) {
        timer = 0;
        String msg = event.getMessage().getString();

        Matcher Matcher = MathPattern.matcher(msg);
        if(Matcher.find()){

            answer = String.format("%.0f", eval(Matcher.group(1)));
        }
        Matcher = WordPattern.matcher(msg);
        if(Matcher.find()){
            answer = findword(Matcher.group(1)).toLowerCase();
        }



        // info(msg);
    }
    public static String findword(String Iword){
        Iword = Iword.toLowerCase();
        String CWord = "";
        int len = Iword.length();

        for (int i = 0; i < Registry.ITEM.size() - 1; i++) {
            CWord = Registry.ITEM.get(i).getName().getString().toLowerCase();
            if(CWord.length() != len) continue;

            for (char c : Iword.toCharArray()) {
                if (CWord == "") {CWord = "null"; break;}
                if(CWord.indexOf(c) != -1)  {CWord = CWord.replaceFirst(""+c, "");}
                else break;
            }
            if(CWord == "") return Registry.ITEM.get(i).getName().getString();
        }

        for (int i = 0; i < Registry.ENTITY_TYPE.size() - 1; i++) {
            CWord = Registry.ENTITY_TYPE.get(i).getName().getString().toLowerCase();
            if(CWord.length() != len) continue;

            for (char c : Iword.toCharArray()) {
                if (CWord == "") {CWord = "null"; break;}
                if(CWord.indexOf(c) != -1)  {CWord = CWord.replaceFirst(""+c, "");}
                else break;
            }
            if(CWord == "") return Registry.ENTITY_TYPE.get(i).getName().getString();
        }

        return null;
    }
    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        if(blockchattting.get()) event.cancel();
    }





    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number
            //        | functionName `(` expression `)` | functionName factor
            //        | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
