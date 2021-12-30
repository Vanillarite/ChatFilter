package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.config.PrefixKind;
import com.vanillarite.filter.util.MemoizedChatMessage;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public
record Warn(
    String warning
) implements Punishment {
  public PunishAction action() {
    return PunishAction.Warn;
  }

  @Override
  public void run(ChatFilter plugin, PlayerEvent chat, MemoizedChatMessage message) {
    plugin.prefixFor(chat.getPlayer(), PrefixKind.WARNING).logged(warning);
  }
}
