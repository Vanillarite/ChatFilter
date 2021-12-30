package com.vanillarite.filter.filters;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.Duration;

@ConfigSerializable
public record Spam(
    String immunePermission,
    Duration timeout,
    Check[] checks
) implements Filter, MultiCheck {
  @Override
  public int buffer() {
    int buffer = 0;
    for (final var check : this.checks) {
      if (check.matchesRequired() > buffer) buffer = check.matchesRequired();
    }
    return buffer;
  }
}
