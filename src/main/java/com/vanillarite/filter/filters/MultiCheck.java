package com.vanillarite.filter.filters;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import org.bukkit.event.player.PlayerEvent;

public interface MultiCheck {
  int buffer();

  Check[] checks();

  default void punish(int violations, ChatFilter plugin, PlayerEvent chat, MemoizedChatMessage message) {
    for (final var check : this.checks()) {
      if (violations >= check.matchesRequired()) check.punish(plugin, chat, message);
    }
  }
}
