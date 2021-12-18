package com.vanillarite.filter.listener;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public record ChatListener(ChatFilter plugin) implements Listener {

  private static <T> void shift(T[] array, T incoming) {
    int size = array.length;
    if (size == 0) return;

    System.arraycopy(array, 0, array, 1, size - 1);
    array[0] = incoming;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent chat) {
    final var player = chat.getPlayer();
    final var message = new MemoizedChatMessage(chat.message());

    for (final var trigger : plugin.config().triggers()) {
      if (trigger.notImmune(player)) {
        if (message.string().contains(trigger.message())) {
          trigger.punish().forEach(i -> i.run(plugin, chat));
        }
      }
    }

    for (final var repeated : plugin.config().repeated()) {
      var levenshtein = new LevenshteinDistance();
      if (repeated.notImmune(player)) {
        var buffer = plugin.bufferTable().get(player.getUniqueId(), repeated);
        if (buffer == null) {
          buffer = new String[repeated.buffer()];
          plugin.bufferTable().put(player.getUniqueId(), repeated, buffer);
        }
        int violations = 1;
        for (final var pastMessage : buffer) {
          if (pastMessage == null) continue;
          if (pastMessage.length() < repeated.minLength()) continue;
          double divisor = Math.max(message.string().length(), pastMessage.length());
          double similarity = levenshtein.apply(message.string(), pastMessage) / divisor;
          if ((1.0 - similarity) >= repeated.similarityThreshold()) violations++;
        }
        shift(buffer, message.string());
        for (final var check : repeated.checks()) {
          if (violations == check.matchesRequired()) {
            check.punish().forEach(i -> i.run(plugin, chat));
          }
        }
      }
    }

//    if (ser.serialize(chat.message()).contains(Objects.requireNonNull(section().getString("trigger")))) {
//      final int playtimeSeconds = plugin.playtimeManager().autoPlaytime(chat.getPlayer());
//      final Duration playtime = Duration.ofSeconds(playtimeSeconds);
//      var prefix = plugin.prefixFor(chat.getPlayer(), PrefixKind.WARNING);
//
//      final String rank = plugin.playtimeManager().topDivision(playtime);
//      if (rank.contains(Objects.requireNonNull(section().getString("only_if_rank")))) {
//        try {
//          var target = plugin
//              .getBm()
//              .getPlayerStorage()
//              .queryForId(UUIDUtils.toBytes(chat.getPlayer().getUniqueId()));
//          var actor = plugin.getBm().getPlayerStorage().getConsole();
//          PlayerMuteData mute =
//              new PlayerMuteData(
//                  target, actor, "automated new player spam protection mute", false, false);
//          var created = plugin.getBm().getPlayerMuteStorage().mute(mute);
//          plugin.getLogger().info("MUTE ACTION %s -> %s".formatted(target, created));
//          plugin.networkBroadcast(
//              prefix.component(
//                  section().getString("broadcast"),
//                  Template.template("user", chat.getPlayer().getName())),
//              null);
//        } catch (SQLException e) {
//          e.printStackTrace();
//        }
//      }
//    }
  }
}
