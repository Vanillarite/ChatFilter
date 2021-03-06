package com.vanillarite.filter.punishments;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.util.MemoizedChatMessage;
import org.bukkit.event.player.PlayerEvent;

public sealed interface Punishment permits Announce, Drop, Mute, Warn {
  PunishAction action();
  void run(ChatFilter plugin, PlayerEvent chat, MemoizedChatMessage message);

}
