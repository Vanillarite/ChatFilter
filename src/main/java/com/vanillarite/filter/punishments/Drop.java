package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
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
  public void run(ChatFilter plugin, PlayerEvent chat, MemoizedChatMessage message) {
    if (chat instanceof Cancellable cancellable) {
      plugin.getLogger().info("§6%s§r sent a message which was §cdeleted by the filter§r: %s".formatted(chat.getPlayer().getName(), message.string()));
      cancellable.setCancelled(true);
    }
  }
}
