package com.vanillarite.filter.util;

import java.time.Instant;

public record PastMessage(String message, Instant time) {
  public static PastMessage now(String message) {
    return new PastMessage(message, Instant.now());
  }
}
