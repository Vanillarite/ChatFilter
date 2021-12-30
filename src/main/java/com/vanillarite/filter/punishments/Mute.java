package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public
record Mute(
    String reason
) implements Punishment {
  public PunishAction action() {
    return PunishAction.Mute;
  }

  @Override
  public void run(ChatFilter plugin, PlayerEvent chat, MemoizedChatMessage message) {
    plugin.mute(chat.getPlayer(), this.reason);
  }
}
