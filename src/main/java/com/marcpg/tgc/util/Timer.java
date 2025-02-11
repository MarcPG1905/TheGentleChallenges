package com.marcpg.tgc.util;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.tgc.challenge.Challenge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Timer {
    public static final TextColor EFFECT_C1 = TextColor.color(255, 215, 0);
    public static final TextColor EFFECT_C2 = TextColor.color(150, 125, 0);
    public static final double EFFECT_SPEED = 0.05;

    private final Challenge challenge;

    private final Time timer = new Time(0);
    private long timerTicks = 0;
    private double phase = 0.0d;
    private boolean reversed = false;

    public Timer(Challenge challenge) {
        this.challenge = challenge;
    }

    public void tick() {
        // The timer in ticks:
        if (challenge.running) {
            timerTicks++;
            if (timerTicks % 20 == 0) {
                if (reversed) {
                    timer.decrement();
                } else {
                    timer.increment();
                }
                challenge.customSecondTick();
            }
        }

        // The animation phase:
        phase += EFFECT_SPEED;
        if (phase >= 1.0)
            phase = -1.0 + (phase - 1.0);

        // The actual animation:
        String text = challenge.running || challenge.beaten ? timer.getPreciselyFormatted() : "Der Timer ist pausiert!";
        Component actionBarText = MiniMessage.miniMessage().deserialize(String.format("<bold><gradient:%s:%s:%f>%s</gradient></bold>", EFFECT_C1.asHexString(), EFFECT_C2.asHexString(), phase, text));
        Challenge.forEachPlayer(p -> {
            p.sendActionBar(actionBarText);
            challenge.playerTick(p);
        });
    }

    public final Time timer() {
        return timer;
    }

    public final boolean reversed() {
        return reversed;
    }

    public final void reversed(boolean reversed) {
        this.reversed = reversed;
    }
}
