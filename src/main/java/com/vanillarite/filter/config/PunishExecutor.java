package com.vanillarite.filter.config;

import com.vanillarite.filter.ChatFilter;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.ArrayList;

public interface PunishExecutor {
  ArrayList<Punishment> punish();

  default void punish(ChatFilter plugin, AsyncChatEvent chat) {
    this.punish().forEach(i -> i.run(plugin, chat));
  }
}
