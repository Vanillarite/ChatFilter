package com.vanillarite.filter.filters;

import com.vanillarite.filter.punishments.PunishExecutor;
import com.vanillarite.filter.punishments.Punishment;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.regex.Pattern;

@ConfigSerializable
public record Trigger(
    String immunePermission,
    Pattern[] regex,
    ArrayList<Punishment> punish
) implements Filter, PunishExecutor {
}
