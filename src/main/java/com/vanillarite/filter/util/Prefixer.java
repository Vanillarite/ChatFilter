package com.vanillarite.filter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.placeholder.Placeholder;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.vanillarite.filter.ChatFilter.m;

public class Prefixer {
  private final @NotNull String miniMessagePrefix;
  private final CommandSender sender;
  private @Nullable Component prefix = null;

  public Prefixer(CommandSender sender, @NotNull String miniMessagePrefix) {
    this.sender = sender;
    this.miniMessagePrefix = miniMessagePrefix;
  }

  private Component prefixMemo() {
    if (prefix == null) {
      prefix = m.deserialize(miniMessagePrefix);
    }
    return prefix;
  }

  private void loggedSend(Component message) {
    sender.sendMessage(message);
    if (sender instanceof Player) {
      Bukkit.getConsoleSender().sendMessage(
          Component.text("[ChatFilter] For " + sender.getName() + ": ").append(message)
      );
    }
  }

  public void logged(Component message) {
    var component = Component.join(JoinConfiguration.noSeparators(), prefixMemo(), message);
    loggedSend(component);
  }

  public void response(Component message) {
    var component = Component.join(JoinConfiguration.noSeparators(), prefixMemo(), message);
    sender.sendMessage(component);
  }

  public void logged(String miniMessage) {
    var component = m.deserialize(miniMessagePrefix + miniMessage);
    loggedSend(component);
  }

  public Component component(String miniMessage, Placeholder<?>... placeholders) {
    return m.deserialize(miniMessagePrefix + miniMessage, PlaceholderResolver.placeholders(placeholders));
  }

  public Component component(String miniMessage, List<Placeholder<?>> placeholders) {
    return m.deserialize(miniMessagePrefix + miniMessage, PlaceholderResolver.placeholders(placeholders));
  }

  public void logged(String miniMessage, Placeholder<?>... templates) {
    loggedSend(component(miniMessage, templates));
  }

  public void response(String miniMessage, Placeholder<?>... templates) {
    sender.sendMessage(component(miniMessage, templates));
  }

  public void response(String miniMessage, List<Placeholder<?>> templates) {
    sender.sendMessage(component(miniMessage, templates));
  }
}
