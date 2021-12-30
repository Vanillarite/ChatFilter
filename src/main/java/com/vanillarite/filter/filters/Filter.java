package com.vanillarite.filter.filters;

import org.bukkit.permissions.Permissible;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.Duration;

public interface Filter {
  String immunePermission();
  default boolean notImmune(Permissible player) {
    return !player.hasPermission("chatfilter.immune." + immunePermission());
  }

}
