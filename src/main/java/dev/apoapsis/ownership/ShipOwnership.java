package dev.apoapsis.ownership;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Who owns a ship. Assembly claims the ship for the player who built it; after that only the owner, a player on the
 * owner's scoreboard team, or an operator (permission level 2) may board it or turn it back into blocks. The orbit
 * scenario claims its ship for the player who ran it, and a ship rebuilt on a surface-to-orbit crossing keeps its owner
 * ({@link #copy}). A ship with no owner (saved before ownership existed, or spawned with no player to claim it) stays
 * open to everyone.
 *
 * <p>Server side only. The client sends input and never decides; the ownership attachment is not synced to clients.
 */
public final class ShipOwnership {
    private ShipOwnership() {}

    /** The owner of a ship: the account's UUID, and the name it had when it claimed the ship (the scoreboard key). */
    public record Owner(UUID id, String name) {
        public static final MapCodec<Owner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Owner::id),
                Codec.STRING.fieldOf("name").forGetter(Owner::name)
        ).apply(i, Owner::new));
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Apoapsis.MODID);

    /**
     * The owner of a {@code ShipEntity}, saved with it. Absent until claimed: always read it with
     * {@code getExistingData}, never {@code getData}, which would store the placeholder default below as an owner.
     */
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Owner>> OWNER = ATTACHMENTS.register(
            "ship_owner",
            () -> AttachmentType.builder(() -> new Owner(new UUID(0L, 0L), "")).serialize(Owner.CODEC).build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    /** The owner of {@code ship}, or empty if it has none. */
    public static Optional<Owner> ownerOf(ShipEntity ship) {
        return ship.getExistingData(OWNER);
    }

    /** Makes {@code player} the owner of {@code ship}, unless the ship already has one or there is no player. */
    public static void claim(ShipEntity ship, Player player) {
        if (player == null || ship.level().isClientSide() || ship.hasData(OWNER)) {
            return;
        }
        ship.setData(OWNER, new Owner(player.getUUID(), player.getScoreboardName()));
    }

    /**
     * Gives {@code to} the owner of {@code from}, if it has one. For a ship rebuilt as a new entity, as on a
     * surface-to-orbit crossing (flight/Transitions.move); without it the rebuilt ship would be open to everyone.
     */
    public static void copy(Entity from, Entity to) {
        from.getExistingData(OWNER).ifPresent(owner -> to.setData(OWNER, owner));
    }

    /** True if {@code player} may take the controls of {@code ship}. Call on the server. */
    public static boolean mayPilot(ShipEntity ship, Player player) {
        return allowed(ship, player);
    }

    /** True if {@code player} may turn {@code ship} back into blocks. Call on the server. */
    public static boolean mayDisassemble(ShipEntity ship, Player player) {
        return allowed(ship, player);
    }

    /** The overlay line for a refused player: "This ship belongs to (owner)." */
    public static Component notYours(ShipEntity ship) {
        return Component.translatable("apoapsis.ownership.not_yours", ownerOf(ship).map(Owner::name).orElse(""));
    }

    private static boolean allowed(ShipEntity ship, Player player) {
        Owner owner = ownerOf(ship).orElse(null);
        if (owner == null || owner.id().equals(player.getUUID())) {
            return true;
        }
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        Scoreboard scoreboard = ship.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        return team != null && team.equals(scoreboard.getPlayersTeam(owner.name()));
    }
}
