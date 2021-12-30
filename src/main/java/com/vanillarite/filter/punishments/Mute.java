package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
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
  public void run(ChatFilter plugin, PlayerEvent chat) {
    plugin.mute(chat.getPlayer(), this.reason);
  }
}
