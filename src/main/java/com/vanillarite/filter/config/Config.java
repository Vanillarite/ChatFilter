package com.vanillarite.filter.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.Map;

@ConfigSerializable
public record Config (
  Map<PrefixKind, String> prefix,
  ArrayList<Filter.Trigger> triggers,
  ArrayList<Filter.Repeated> repeated,
  ArrayList<Filter.Spam> spam,
  ArrayList<Filter.Links> links
) {

}
