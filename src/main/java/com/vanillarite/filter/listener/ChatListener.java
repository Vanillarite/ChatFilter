package com.vanillarite.filter.listener;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public record ChatListener(ChatFilter plugin) implements Listener {

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

    plugin.getLogger().info("%s %s".formatted(chat.isCancelled(), message.string()));

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
