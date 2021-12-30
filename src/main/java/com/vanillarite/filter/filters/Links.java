package com.vanillarite.filter.filters;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Links(
    String immunePermission,
    int buffer,
    Check[] checks
) implements Filter, MultiCheck {
}
