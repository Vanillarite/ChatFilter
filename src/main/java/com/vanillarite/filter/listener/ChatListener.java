package com.vanillarite.filter.listener;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import com.vanillarite.filter.util.PastMessage;
import info.debatty.java.stringsimilarity.SorensenDice;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

public record ChatListener(ChatFilter plugin) implements Listener {
  public static final SorensenDice similarityChecker = new SorensenDice();
  private static final Pattern urlPattern = Pattern.compile(
      "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
          + "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)+"
          + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:\\/{};']*)",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent chat) {
    final var player = chat.getPlayer();
    final var message = new MemoizedChatMessage(chat.message());

    for (final var trigger : plugin.config().triggers()) {
      if (trigger.notImmune(player)) {
        if (message.string().contains(trigger.message())) trigger.punish(plugin, chat);
      }
    }

    for (final var repeated : plugin.config().repeated()) {
      if (repeated.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), repeated);
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
          double similarity = similarityChecker.similarity(message.string(), pastMessage.message());
          if (similarity >= repeated.similarityThreshold()) violations++;
        }
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        repeated.punish(violations, plugin, chat);
      }
    }

    for (final var link : plugin.config().links()) {
      if (link.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), link);
        if (buffer == null) {
          buffer = new PastMessage[link.buffer()];
          plugin.bufferTable().put(player.getUniqueId(), link, buffer);
        }
        int violations = 0;
        var now = Instant.now();
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        for (final var pastMessage : buffer) {
          if (pastMessage == null) continue;
          boolean foundViolation = false;
          // if (Duration.between(pastMessage.time(), now).compareTo(link.timeout()) > 0) continue; // Do we want timeouts on link detection?
          for (final var messagePart : pastMessage.message().split("\\s+")) {
            if (urlPattern.matcher(messagePart).matches()) foundViolation = true;
          }
          if (foundViolation) violations++; // extra indirection to avoid multiple increments per message
        }
        link.punish(violations, plugin, chat);
      }
    }
  }
}
