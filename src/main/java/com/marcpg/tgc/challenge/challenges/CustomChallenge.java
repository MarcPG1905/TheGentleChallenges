package com.marcpg.tgc.challenge.challenges;

import com.marcpg.tgc.challenge.Challenge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CustomChallenge extends Challenge {
    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#888888:#BBBBBB>Custom Challenge</gradient></bold>");
    }
}
