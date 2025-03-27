package com.vanillarite.filter.commands;

import com.vanillarite.filter.ChatFilter;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;

public record Commands(ChatFilter plugin) {
  @Command("chatfilter reload")
  @Permission("chatfilter.reload")
  private void commandReload(final @NotNull CommandSender sender) {
    try {
      plugin.loadConfig();
      plugin.bufferTable().clear();
      plugin.violationsTable().clear();
      sender.sendMessage("[ChatFilter] Reloaded!!");
    } catch (ConfigurateException ex) {
      sender.sendMessage("[ChatFilter] Couldn't reload! Check the console for errors");
      ex.printStackTrace();
    }
  }

  @Command("chatfilter toggle [state]")
  @Permission("chatfilter.toggle")
  private void commandToggle(
      final @NotNull CommandSender sender,
      final @Argument("state") @Default("false") @NotNull String state
  ) {
    boolean newState = state.equals("on");
    plugin.state(newState);
    sender.sendMessage("[ChatFilter] Filtering is now " + (newState ? "ON" : "OFF"));
  }
}
