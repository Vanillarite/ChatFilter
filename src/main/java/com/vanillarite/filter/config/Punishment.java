package com.vanillarite.filter.config;

import com.vanillarite.filter.ChatFilter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.placeholder.Placeholder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public sealed interface Punishment {
  Punishment.Action action();
  void run(ChatFilter plugin, PlayerEvent chat);

  enum Action {
    Warn, Mute, Drop, Announce
  }

  @ConfigSerializable
  record Warn(
      String warning
  ) implements Punishment {
    public Punishment.Action action() { return Action.Warn; }

    @Override
    public void run(ChatFilter plugin, PlayerEvent chat) {
      plugin.prefixFor(chat.getPlayer(), PrefixKind.WARNING).logged(warning);
    }
  }

  @ConfigSerializable
  record Mute(
      String reason
  ) implements Punishment {
    public Punishment.Action action() { return Action.Mute; }

    @Override
    public void run(ChatFilter plugin, PlayerEvent chat) {
      plugin.mute(chat.getPlayer(), this.reason);
    }
  }

  @ConfigSerializable
  record Drop() implements Punishment {
    public Punishment.Action action() { return Action.Drop; }

    @Override
    public void run(ChatFilter plugin, PlayerEvent chat) {
      if (chat instanceof Cancellable cancellable) cancellable.setCancelled(true);
    }
  }

  @ConfigSerializable
  record Announce(
      String message
  ) implements Punishment {
    public Punishment.Action action() { return Action.Announce; }

    @Override
    public void run(ChatFilter plugin, PlayerEvent chat) {
      plugin.networkBroadcast(
          plugin.prefixFor(chat.getPlayer(), PrefixKind.AUTO_ACTION)
              .component(message, Placeholder.component("user", Component.text(chat.getPlayer().getName()))),
          null);
    }
  }
}
