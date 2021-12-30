package com.vanillarite.filter.config;

import com.vanillarite.filter.ChatFilter;
import org.bukkit.event.player.PlayerEvent;

import java.util.ArrayList;

public interface PunishExecutor {
  ArrayList<Punishment> punish();

  default void punish(ChatFilter plugin, PlayerEvent chat) {
    this.punish().forEach(i -> i.run(plugin, chat));
  }
}
