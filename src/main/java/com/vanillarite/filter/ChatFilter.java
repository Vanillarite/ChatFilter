package com.vanillarite.filter;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.vanillarite.filter.commands.Commands;
import com.vanillarite.filter.config.Config;
import com.vanillarite.filter.config.serializers.DurationSerializer;
import com.vanillarite.filter.filters.MultiCheck;
import com.vanillarite.filter.config.PrefixKind;
import com.vanillarite.filter.punishments.Punishment;
import com.vanillarite.filter.config.serializers.PunishmentSerializer;
import com.vanillarite.filter.listener.ChatListener;
import com.vanillarite.filter.util.PastMessage;
import com.vanillarite.filter.util.Prefixer;
import me.confuser.banmanager.bukkit.BMBukkitPlugin;
import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.data.PlayerMuteData;
import me.confuser.banmanager.common.util.UUIDUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

public final class ChatFilter extends JavaPlugin implements Listener {
  public static final MiniMessage m = MiniMessage.miniMessage();
  public static final UUID uuid = UUID.fromString("cea5e000-6a98-58d0-ad28-28874d04db21");
  private PlayerData actor;
  private boolean isBungee = false;
  private Config config;
  private static final ObjectMapper.Factory objectFactory = ObjectMapper
      .factoryBuilder()
      .defaultNamingScheme(NamingSchemes.SNAKE_CASE)
      .build();
  private final YamlConfigurationLoader.Builder configBuilder = YamlConfigurationLoader.builder()
      .defaultOptions(opts -> opts.serializers(builder -> builder
          .registerAnnotatedObjects(objectFactory)
          .register(Punishment.class, PunishmentSerializer.INSTANCE)
          .register(Duration.class, DurationSerializer.INSTANCE))
      );
  private final Table<UUID, MultiCheck, PastMessage[]> bufferTable = Tables.synchronizedTable(HashBasedTable.create());
  private final Table<UUID, MultiCheck, Integer> violationsTable = Tables.synchronizedTable(HashBasedTable.create());
  private final File configFile = new File(this.getDataFolder(), "config.yml");
  private BanManagerPlugin bm;
  private LibertyBans liberty;
  private boolean state = true;

  public boolean state() {
    return state;
  }

  public void state(boolean state) {
    this.state = state;
  }

  public Config config() {
    return config;
  }

  public Table<UUID, MultiCheck, PastMessage[]> bufferTable() {
    return bufferTable;
  }

  public Table<UUID, MultiCheck, Integer> violationsTable() {
    return violationsTable;
  }

  public static ObjectMapper.Factory objectFactory() {
    return objectFactory;
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();

    isBungee = getServer().spigot().getSpigotConfig().getBoolean("settings.bungeecord");
    if (isBungee) {
      getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    try {
      loadConfig();
      getLogger().info(this.config.toString());
    } catch (ConfigurateException e) {
      e.printStackTrace();
    }

    final LegacyPaperCommandManager<CommandSender> manager =
            LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.simpleCoordinator());
    if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
      manager.registerBrigadier();
    }
    AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(manager, CommandSender.class);
    annotationParser.parse(new Commands(this));

    this.getServer().getPluginManager().registerEvents(this, this);
    this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
  }

  @EventHandler
  public void onServerStartup(ServerLoadEvent e) {
    final var banManager = Bukkit.getPluginManager().getPlugin("BanManager");
    if (banManager instanceof BMBukkitPlugin bmBukkitPlugin) {
      this.bm = bmBukkitPlugin.getPlugin();
      this.setupBmActor();
      this.getLogger().info("Hooked into BanManager (%s), created actor (%s)".formatted(this.bm, this.actor));
    }
    final var libertyBans = Bukkit.getPluginManager().getPlugin("LibertyBans");
    if (libertyBans != null) {
      Omnibus omnibus = OmnibusProvider.getOmnibus();
      this.liberty = omnibus.getRegistry().getProvider(LibertyBans.class).orElseThrow();
      this.getLogger().info("Hooked into LibertyBans (%s)".formatted(this.liberty));
    }
  }

  public void networkBroadcast(@NotNull Component c, @Nullable CommandSender sender) {
    if (!isBungee) {
      getServer().broadcast(c);
      return;
    }

    //noinspection UnstableApiUsage
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("MessageRaw");
    out.writeUTF("ALL");
    out.writeUTF(GsonComponentSerializer.gson().serialize(c));
    Bukkit.getConsoleSender().sendMessage(c);

    Bukkit.getOnlinePlayers().stream()
        .findFirst()
        .ifPresentOrElse(
            (e) -> e.sendPluginMessage(this, "BungeeCord", out.toByteArray()),
            () -> {
              if (sender != null) {
                sender.sendMessage(Component.text("Failed to broadcast because nobody is online", NamedTextColor.RED));
              }
              getLogger().severe("Failed to broadcast because nobody is online");
            });
  }

  public void setupBmActor() {
    try {
      final String name = "(auto)" + this.getName();
      final var storage = this.bm.getPlayerStorage();
      this.actor = storage.queryForId(UUIDUtils.toBytes(uuid));
      if (this.actor == null) {
        this.actor = new PlayerData(uuid, name);
        storage.create(this.actor);
      }
    } catch (SQLException ex) {
      this.getLogger().severe("Failed to setup actor because of %s!!".formatted(ex));
      ex.printStackTrace();
    }
  }

  public void mute(Player player, String reason) {
    if (this.bm != null) {
      this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
        try {
          var storage = this.bm.getPlayerStorage();
          var target = storage.queryForId(UUIDUtils.toBytes(player.getUniqueId()));
          var mute = new PlayerMuteData(target, this.actor, reason, false, false);
          var created = this.bm.getPlayerMuteStorage().mute(mute);
          this.getLogger().info("MUTE ACTION %s -> %s".formatted(target, created));
        } catch (SQLException ex) {
          this.getLogger().severe("Failed to mute %s because of %s!!".formatted(player, ex));
          ex.printStackTrace();
        }
      });
    }
    if (this.liberty != null) {
      this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
        PunishmentDrafter drafter = this.liberty.getDrafter();

        DraftPunishment draftBan = drafter.draftBuilder()
            .type(PunishmentType.MUTE)
            .victim(PlayerVictim.of(player.getUniqueId()))
            .reason(reason).build();

        draftBan.enactPunishment().thenAcceptAsync((punishment) -> punishment.ifPresentOrElse(
            (p) -> this.getLogger().info("ID of the enacted punishment is %s".formatted(p.getIdentifier())),
            ( ) -> this.getLogger().warning("Player %s/%s is already muted?".formatted(player.getName(), player.getUniqueId()))
        ));
      });
    }
    if (this.liberty == null && this.bm == null) {
      this.getLogger().severe("Cannot mute %s/%s because no plugin is setup to handle mutes!".formatted(player.getName(), player.getUniqueId()));
    }
  }

  public boolean isMuted(Player player) {
    if (this.bm != null) {
      return this.bm.getPlayerMuteStorage().isMuted(player.getUniqueId());
    }
    return false;
  }

  public static <T> void shift(T[] array, T incoming) {
    int size = array.length;
    if (size == 0) return;

    System.arraycopy(array, 0, array, 1, size - 1);
    array[0] = incoming;
  }

  public Prefixer prefixFor(CommandSender sender, PrefixKind kind) {
    return new Prefixer(sender, config.prefix().get(kind));
  }

  public void loadConfig() throws ConfigurateException {
    this.config = objectFactory.get(Config.class).load(configBuilder.file(configFile).build().load());
  }
}
