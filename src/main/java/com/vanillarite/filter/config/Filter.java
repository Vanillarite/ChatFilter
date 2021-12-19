package com.vanillarite.filter.config;

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
  ) implements Filter {
    @ConfigSerializable
    public record Check(
        int matchesRequired,
        ArrayList<Punishment> punish
    ) implements PunishExecutor {

    }
  }

  @ConfigSerializable
  record Links(
      String immunePermission,
      int buffer
  ) implements Filter {
  }
}
