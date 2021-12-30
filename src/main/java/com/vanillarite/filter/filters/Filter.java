package com.vanillarite.filter.filters;

import org.bukkit.permissions.Permissible;

public interface Filter {
  String immunePermission();
  default boolean notImmune(Permissible player) {
    return !player.hasPermission("chatfilter.immune." + immunePermission());
  }

}
