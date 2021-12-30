package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.config.PrefixKind;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.placeholder.Placeholder;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public
record Announce(
    String message
) implements Punishment {
  public PunishAction action() {
    return PunishAction.Announce;
  }

  @Override
  public void run(ChatFilter plugin, PlayerEvent chat) {
    plugin.networkBroadcast(
        plugin.prefixFor(chat.getPlayer(), PrefixKind.AUTO_ACTION)
            .component(message, Placeholder.component("user", Component.text(chat.getPlayer().getName()))),
        null);
  }
}
