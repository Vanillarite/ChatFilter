package com.vanillarite.filter.config;

import com.vanillarite.filter.ChatFilter;
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
    final var action = actionNode.get(Punishment.Action.class);
    final var punishmentClass = switch (Objects.requireNonNull(action)) {
      case Warn -> Punishment.Warn.class;
      case Mute -> Punishment.Mute.class;
      case Drop -> Punishment.Drop.class;
      case Announce -> Punishment.Announce.class;
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