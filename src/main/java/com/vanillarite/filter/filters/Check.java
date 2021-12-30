package com.vanillarite.filter.filters;

import com.vanillarite.filter.punishments.PunishExecutor;
import com.vanillarite.filter.punishments.Punishment;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;

@ConfigSerializable
public record Check(
    int matchesRequired,
    ArrayList<Punishment> punish
) implements PunishExecutor {
}
