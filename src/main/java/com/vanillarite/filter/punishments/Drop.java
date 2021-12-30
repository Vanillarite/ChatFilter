package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public
record Drop() implements Punishment {
  public PunishAction action() {
    return PunishAction.Drop;
  }

  @Override
  public void run(ChatFilter plugin, PlayerEvent chat) {
    if (chat instanceof Cancellable cancellable) cancellable.setCancelled(true);
  }
}
