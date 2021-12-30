package com.vanillarite.filter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MemoizedChatMessage {
  private static final PlainTextComponentSerializer ser = PlainTextComponentSerializer.plainText();
  private final Component component;
  private String string = null;

  public MemoizedChatMessage(Component component) {
    this.component = component;
  }

  public MemoizedChatMessage(String string) {
    this.component = null;
    this.string = string;
  }

  public @NotNull String string() {
    if (this.string == null) {
      this.string = ser.serialize(Objects.requireNonNull(this.component));
    }
    return this.string;
  }
}
