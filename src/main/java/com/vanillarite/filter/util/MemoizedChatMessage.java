package com.vanillarite.filter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class MemoizedChatMessage {
  private static final PlainTextComponentSerializer ser = PlainTextComponentSerializer.plainText();
  private final Component component;
  private String string = null;

  public MemoizedChatMessage(Component component) {
    this.component = component;
  }

  public @NotNull String string() {
    if (this.string == null) {
      this.string = ser.serialize(this.component);
    }
    return this.string;
  }
}
