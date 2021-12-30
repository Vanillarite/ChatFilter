package com.vanillarite.filter.config;

import com.vanillarite.filter.ChatFilter;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.permissions.Permissible;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.Duration;
import java.util.ArrayList;

public interface Filter {
  String immunePermission();
  default boolean notImmune(Permissible player) {
    return !player.hasPermission("chatfilter.immune." + immunePermission());
  }

  @ConfigSerializable
  record Check(
      int matchesRequired,
      ArrayList<Punishment> punish
  ) implements PunishExecutor {
  }

  interface MultiCheck {
    int buffer();
    Check[] checks();
    default void punish(int violations, ChatFilter plugin, PlayerEvent chat) {
      for (final var check : this.checks()) {
        if (violations >= check.matchesRequired()) check.punish(plugin, chat);
      }
    }
  }

  @ConfigSerializable
  record Trigger(
      String immunePermission,
      String message,
      ArrayList<Punishment> punish
  ) implements Filter, PunishExecutor {
  }

  @ConfigSerializable
  record Repeated(
      String immunePermission,
      int buffer,
      Duration timeout,
      double similarityThreshold,
      int minLength,
      Check[] checks
  ) implements Filter, MultiCheck {
  }

  @ConfigSerializable
  record Spam(
      String immunePermission,
      Duration timeout,
      Check[] checks
  ) implements Filter, MultiCheck {
    @Override
    public int buffer() {
      int buffer = 0;
      for (final var check : this.checks) {
        if (check.matchesRequired > buffer) buffer = check.matchesRequired;
      }
      return buffer;
    }
  }

  @ConfigSerializable
  record Links(
      String immunePermission,
      int buffer,
      Check[] checks
  ) implements Filter, MultiCheck {
  }
}
