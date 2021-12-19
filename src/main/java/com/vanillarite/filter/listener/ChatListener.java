package com.vanillarite.filter.listener;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import com.vanillarite.filter.util.PastMessage;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;

public record ChatListener(ChatFilter plugin) implements Listener {
  public static final LevenshteinDistance levenshtein = new LevenshteinDistance();

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
          double divisor = Math.max(message.string().length(), pastMessage.message().length());
          double similarity = levenshtein.apply(message.string(), pastMessage.message()) / divisor;
          if ((1.0 - similarity) >= repeated.similarityThreshold()) violations++;
        }
        ChatFilter.shift(buffer, PastMessage.now(message.string()));
        for (final var check : repeated.checks()) {
          if (violations == check.matchesRequired()) check.punish(plugin, chat);
        }
      }
    }
  }
}
