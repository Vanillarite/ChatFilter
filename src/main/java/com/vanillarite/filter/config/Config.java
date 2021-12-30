package com.vanillarite.filter.config;

import com.vanillarite.filter.filters.Links;
import com.vanillarite.filter.filters.Repeated;
import com.vanillarite.filter.filters.Spam;
import com.vanillarite.filter.filters.Trigger;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.Map;

@ConfigSerializable
public record Config (
  Map<PrefixKind, String> prefix,
  ArrayList<Trigger> triggers,
  ArrayList<Repeated> repeated,
  ArrayList<Spam> spam,
  ArrayList<Links> links
) {

}
