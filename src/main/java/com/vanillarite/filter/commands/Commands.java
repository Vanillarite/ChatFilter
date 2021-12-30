package com.vanillarite.filter.commands;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.vanillarite.filter.ChatFilter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;

public record Commands(ChatFilter plugin) {
  @CommandMethod("chatfilter reload")
  @CommandPermission("chatfilter.reload")
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

  @CommandMethod("chatfilter toggle [state]")
  @CommandPermission("chatfilter.toggle")
  private void commandToggle(
      final @NotNull CommandSender sender,
      final @Argument(value = "state", defaultValue = "false") @NotNull String state
  ) {
    boolean newState = state.equals("on");
    plugin.state(newState);
    sender.sendMessage("[ChatFilter] Filtering is now " + (newState ? "ON" : "OFF"));
  }
}
