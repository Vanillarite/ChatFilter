package com.vanillarite.filter.listener;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import com.vanillarite.filter.util.PastMessage;
import info.debatty.java.stringsimilarity.OptimalStringAlignment;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

public record ChatListener(ChatFilter plugin) implements Listener {
  public static final StringDistance similarityChecker = new OptimalStringAlignment();
  private static final Pattern urlPattern = Pattern.compile(
      "(?:^|[\\W])(https?://|www\\.|play\\.)"
          + "(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)+"
          + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern urlPattern2 = Pattern.compile(
      "(?:^|[\\W])(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)+"
          + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)"
          + "(\\.(com|tk|ml|gg)|:\\d+)",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern ipPattern = Pattern.compile(
      "\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\b",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

  // Knowingly using deprecated chat event because of legacy plugin incompatibility -- as AsyncPlayerChatEvent is fired
  // before AsyncChatEvent, I have no way of cancelling the chat event before a legacy listener using
  // AsyncPlayerChatEvent has already processed it...
  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(@SuppressWarnings("deprecation") AsyncPlayerChatEvent chat) {
    if (!plugin.state()) return;
    if (chat.getMessage().startsWith("/")) return;

    final var player = chat.getPlayer();
    if (plugin.isMuted(player)) return;

    // MemoizedChatMessage was only useful when AsyncChatEvent was still used, but I'm too lazy to get rid of it now.
    final var message = new MemoizedChatMessage(chat.getMessage());

    for (final var trigger : plugin.config().triggers()) {
      if (trigger.notImmune(player)) {
        for (final var pattern : trigger.regex()) {
          if (pattern.matcher(message.string()).find()) {
            trigger.punish(plugin, chat, message);
            break;
          }
        }
      }
    }

    for (final var repeated : plugin.config().repeated()) {
      if (repeated.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), repeated);
        var previousViolations = plugin.violationsTable().get(player.getUniqueId(), repeated);
        if (previousViolations == null) previousViolations = 0;
        if (buffer == null) {
          buffer = new PastMessage[repeated.buffer()];
          plugin.bufferTable().put(player.getUniqueId(), repeated, buffer);
        }
        int violations = 1;
        var now = Instant.now();
        for (final var pastMessage : buffer) {
          if (pastMessage == null) continue;
          if (Duration.between(pastMessage.time(), now).compareTo(repeated.timeout()) > 0) continue;
          if (pastMessage.message().length() < repeated.minLength()) continue;
          int longest = Math.max(message.string().length(), pastMessage.message().length());
          double similarity = similarityChecker.distance(message.string(), pastMessage.message()) / longest;
          if ((1.0 - similarity) >= repeated.similarityThreshold()) violations++;
        }
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        if (violations > previousViolations) repeated.punish(violations, plugin, chat, message);
        plugin.violationsTable().put(player.getUniqueId(), repeated, violations);
      }
    }

    for (final var spam : plugin.config().spam()) {
      if (spam.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), spam);
        var previousViolations = plugin.violationsTable().get(player.getUniqueId(), spam);
        if (previousViolations == null) previousViolations = 0;
        if (buffer == null) {
          buffer = new PastMessage[spam.buffer()];
          plugin.bufferTable().put(player.getUniqueId(), spam, buffer);
        }
        int violations = 0;
        var now = Instant.now();
        for (final var pastMessage : buffer) {
          if (pastMessage == null) continue;
          if (Duration.between(pastMessage.time(), now).compareTo(spam.timeout()) > 0) continue;
          violations++;
        }
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        if (violations > previousViolations) spam.punish(violations, plugin, chat, message);
        plugin.violationsTable().put(player.getUniqueId(), spam, violations);
      }
    }

    for (final var link : plugin.config().links()) {
      if (link.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), link);
        var previousViolations = plugin.violationsTable().get(player.getUniqueId(), link);
        if (previousViolations == null) previousViolations = 0;
        if (buffer == null) {
          buffer = new PastMessage[link.buffer()];
          plugin.bufferTable().put(player.getUniqueId(), link, buffer);
        }
        int violations = 0;
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        for (final var pastMessage : buffer) {
          if (pastMessage == null) continue;
          boolean foundViolation = false;
          // if (Duration.between(pastMessage.time(), now).compareTo(link.timeout()) > 0) continue; // Do we want timeouts on link detection?
          for (final var messagePart : pastMessage.message().split("\\s+")) { // Links can't contain spaces
            if (urlPattern.matcher(messagePart).matches()) foundViolation = true;
            if (urlPattern2.matcher(messagePart).matches()) foundViolation = true;
            if (ipPattern.matcher(messagePart).matches()) foundViolation = true;
          }
          if (foundViolation) violations++; // extra indirection to avoid multiple increments per message
        }
        if (violations > previousViolations) link.punish(violations, plugin, chat, message);
        plugin.violationsTable().put(player.getUniqueId(), link, violations);
      }
    }
  }
}
