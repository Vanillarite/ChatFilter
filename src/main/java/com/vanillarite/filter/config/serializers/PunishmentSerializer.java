package com.vanillarite.filter.config.serializers;

import com.vanillarite.filter.ChatFilter;
import com.vanillarite.filter.punishments.PunishAction;
import com.vanillarite.filter.punishments.Announce;
import com.vanillarite.filter.punishments.Drop;
import com.vanillarite.filter.punishments.Mute;
import com.vanillarite.filter.punishments.Punishment;
import com.vanillarite.filter.punishments.Warn;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public class PunishmentSerializer implements TypeSerializer<Punishment> {
  public static final PunishmentSerializer INSTANCE = new PunishmentSerializer();

  private static final String ACTION = "action";

  private PunishmentSerializer() {
  }

  private ConfigurationNode nonVirtualNode(final ConfigurationNode source, final Object... path) throws SerializationException {
    if (!source.hasChild(path)) {
      throw new SerializationException("Required field " + Arrays.toString(path) + " was not present in node");
    }
    return source.node(path);
  }

  @Override
  public Punishment deserialize(final Type type, final ConfigurationNode source) throws SerializationException {
    final ConfigurationNode actionNode = nonVirtualNode(source, ACTION);
    final var action = actionNode.get(PunishAction.class);
    final var punishmentClass = switch (Objects.requireNonNull(action)) {
      case Warn -> Warn.class;
      case Mute -> Mute.class;
      case Drop -> Drop.class;
      case Announce -> Announce.class;
    };
    return ChatFilter.objectFactory().get(punishmentClass).load(source);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void serialize(final Type type, final @Nullable Punishment punish, final ConfigurationNode target) throws SerializationException {
    if (punish == null) {
      target.raw(null);
      return;
    }

    // Suck my dick, javac
    @SuppressWarnings("rawtypes")
    final ObjectMapper mapper = ChatFilter.objectFactory().get(punish.getClass());
    mapper.save(punish, target);

    target.node(ACTION).set(punish.action());
  }
}