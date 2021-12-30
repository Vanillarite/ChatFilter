package com.vanillarite.filter.config.serializers;

import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Predicate;

public class DurationSerializer extends ScalarSerializer<Duration> {
  public static final DurationSerializer INSTANCE = new DurationSerializer();

  protected DurationSerializer() {
    super(Duration.class);
  }

  @Override
  public Duration deserialize(Type type, Object value) throws SerializationException {
    final String potential = value.toString().toUpperCase(Locale.ROOT).replace(" ", "");
    return Duration.parse("PT" + potential);
  }

  @Override
  protected Object serialize(Duration item, Predicate<Class<?>> typeSupported) {
    return null;
  }
}
