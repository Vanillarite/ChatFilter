package com.vanillarite.filter.filters;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.Duration;

@ConfigSerializable
public record Repeated(
    String immunePermission,
    int buffer,
    Duration timeout,
    double similarityThreshold,
    int minLength,
    Check[] checks
) implements Filter, MultiCheck {
}
