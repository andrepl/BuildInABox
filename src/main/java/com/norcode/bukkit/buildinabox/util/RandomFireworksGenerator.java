package com.norcode.bukkit.buildinabox.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Random;

public class RandomFireworksGenerator {
    private static Random rand = new Random();

    public static void assignRandomFireworkMeta(Firework fw) {
        FireworkMeta fwm = fw.getFireworkMeta();
        FireworkEffect.Type type = getRandomType();
        Color c1 = getRandomColor();
        Color c2 = getRandomColor();
        FireworkEffect effect = FireworkEffect.builder()
            .flicker(rand.nextBoolean())
            .withColor(c1)
            .withFade(c2)
            .with(type)
            .trail(rand.nextBoolean())
            .build();
        //Then apply the effect to the meta
        fwm.addEffect(effect);
        //Generate some random power and set it
        int rp = rand.nextInt(2)+1;
        fwm.setPower(rp);
        //Then apply this to our rocket
        fw.setFireworkMeta(fwm);
    }

    private static Type getRandomType() {
        int idx = rand.nextInt(FireworkEffect.Type.values().length);
        return FireworkEffect.Type.values()[idx];
    }

    private static Color getRandomColor() {
        switch (rand.nextInt(17)) {
        case 0: return Color.AQUA;
        case 1: return Color.BLACK;
        case 2: return Color.BLUE;
        case 3: return Color.FUCHSIA;
        case 4: return Color.GRAY;
        case 5: return Color.GREEN;
        case 6: return Color.LIME;
        case 7: return Color.MAROON;
        case 8: return Color.NAVY;
        case 9: return Color.OLIVE;
        case 10: return Color.ORANGE;
        case 11: return Color.PURPLE;
        case 12: return Color.RED;
        case 13: return Color.SILVER;
        case 14: return Color.TEAL;
        case 15: return Color.WHITE;
        case 16: return Color.YELLOW;
        }
        return Color.WHITE;
    }
}
