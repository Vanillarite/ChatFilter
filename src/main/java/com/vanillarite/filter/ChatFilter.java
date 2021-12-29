package com.vanillarite.filter;

import cloud.commandframework.CommandTree;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.vanillarite.filter.config.Config;
import com.vanillarite.filter.config.DurationSerializer;
import com.vanillarite.filter.config.Filter;
import com.vanillarite.filter.config.PrefixKind;
import com.vanillarite.filter.config.Punishment;
import com.vanillarite.filter.config.PunishmentSerializer;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

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
  private final Table<UUID, Filter.MultiCheck, PastMessage[]> bufferTable = Tables.synchronizedTable(HashBasedTable.create());
  private final File configFile = new File(this.getDataFolder(), "config.yml");
  private BanManagerPlugin bm;

  public Config config() {
    return config;
  }

  public Table<UUID, Filter.MultiCheck, PastMessage[]> bufferTable() {
    return bufferTable;
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

    final Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>>
        executionCoordinatorFunction =
        AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder().build();
    final Function<CommandSender, CommandSender> mapperFunction = Function.identity();
    PaperCommandManager<CommandSender> manager;
    try {
      manager =
          new PaperCommandManager<>(
              this, executionCoordinatorFunction, mapperFunction, mapperFunction);
    } catch (final Exception e) {
      getLogger().severe("Failed to initialize the command manager");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    manager.registerBrigadier();
    manager.registerAsynchronousCompletions();
    manager.setCommandSuggestionProcessor((context, strings) -> {
      final String input;
      if (context.getInputQueue().isEmpty()) {
        input = "";
      } else {
        input = context.getInputQueue().peek();
      }
      final List<String> suggestions = new LinkedList<>();
      for (final String suggestion : strings) {
        if (suggestion.toLowerCase().startsWith(input.toLowerCase())) {
          suggestions.add(suggestion);
        }
      }
      return suggestions;
    });
    final Function<ParserParameters, CommandMeta> commandMetaFunction =
        p ->
            CommandMeta.simple()
                .with(
                    CommandMeta.DESCRIPTION,
                    p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();
    AnnotationParser<CommandSender> annotationParser =
        new AnnotationParser<>(manager, CommandSender.class, commandMetaFunction);

    this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
  }

  @EventHandler
  public void onServerStartup(ServerLoadEvent e) {
    this.bm =
        Objects.requireNonNull((BMBukkitPlugin) Bukkit.getPluginManager().getPlugin("BanManager"))
            .getPlugin();
    this.setupBmActor();
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
              } else {
                getLogger().severe("Failed to broadcast because nobody is online");
              }
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
