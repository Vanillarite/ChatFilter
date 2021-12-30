package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.punishments.Punishment;
import org.bukkit.event.player.PlayerEvent;

import java.util.ArrayList;

public interface PunishExecutor {
  ArrayList<Punishment> punish();

  default void punish(ChatFilter plugin, PlayerEvent chat) {
    this.punish().forEach(i -> {
      plugin.getLogger().info("Running punishment %s against %s (%s)".formatted(i.action(), chat.getPlayer().getName(), i.toString()));
      i.run(plugin, chat);
    });
  }
}
