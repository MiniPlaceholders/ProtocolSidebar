package me.catcoder.sidebar;

import com.comphenix.protocol.injector.netty.WirePacket;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import me.catcoder.sidebar.protocol.PacketIds;
import me.catcoder.sidebar.text.TextProvider;
import me.catcoder.sidebar.util.ByteBufNetOutput;
import me.catcoder.sidebar.util.NetOutput;
import me.catcoder.sidebar.util.VersionUtil;
import org.bukkit.entity.Player;

import static me.catcoder.sidebar.SidebarLine.sendWirePacket;

/**
 * Encapsulates scoreboard objective
 *
 * @author CatCoder
 * @see <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/scoreboard/Objective.html">Bukkit
 * documentation</a>
 */
@Getter
public class ScoreboardObjective<R> {


    public static final int DISPLAY_SIDEBAR = 1;
    public static final int ADD_OBJECTIVE = 0;
    public static final int REMOVE_OBJECTIVE = 1;
    public static final int UPDATE_VALUE = 2;

    private final String name;
    private final TextProvider<R> textProvider;
    private R displayName;

    ScoreboardObjective(@NonNull String name, @NonNull R displayName, @NonNull TextProvider<R> textProvider) {
        Preconditions.checkArgument(
                name.length() <= 16, "Objective name exceeds 16 symbols limit");

        this.name = name;
        this.textProvider = textProvider;
        this.displayName = displayName;
    }

    void setDisplayName(@NonNull R displayName) {
        this.displayName = displayName;
    }

    void updateValue(@NonNull Player player) {
        WirePacket packet = getPacket(player, UPDATE_VALUE);
        sendWirePacket(player, packet);
    }

    void create(@NonNull Player player) {
        WirePacket packet = getPacket(player, ADD_OBJECTIVE);
        sendWirePacket(player, packet);
    }

    void remove(@NonNull Player player) {
        WirePacket packet = getPacket(player, REMOVE_OBJECTIVE);
        sendWirePacket(player, packet);
    }

    void display(@NonNull Player player) {
        ByteBuf buf = Unpooled.buffer();
        NetOutput output = new ByteBufNetOutput(buf);

        output.writeByte(DISPLAY_SIDEBAR);
        output.writeString(name);

        sendWirePacket(player, new WirePacket(PacketIds.OBJECTIVE_DISPLAY.getPacketId(VersionUtil.SERVER_VERSION), output.toByteArray()));
    }

    private WirePacket getPacket(@NonNull Player player, int mode) {
        int version = VersionUtil.getPlayerVersion(player.getUniqueId());

        ByteBuf buf = Unpooled.buffer();
        NetOutput output = new ByteBufNetOutput(buf);

        output.writeString(name);
        output.writeByte(mode);

        if (mode == ADD_OBJECTIVE || mode == UPDATE_VALUE) {
            String legacyText = textProvider.asLegacyMessage(player, displayName);
            // Since 1.13 characters limit for display name was removed
            if (version < VersionUtil.MINECRAFT_1_13 && legacyText.length() > 32) {
                legacyText = legacyText.substring(0, 32);
            }

            if (VersionUtil.SERVER_VERSION >= VersionUtil.MINECRAFT_1_13) {
                output.writeString(textProvider.asJsonMessage(player, displayName));
            } else {
                output.writeString(legacyText);
            }

            if (VersionUtil.SERVER_VERSION >= VersionUtil.MINECRAFT_1_13) {
                output.writeVarInt(0); // Health display
            } else {
                output.writeString("integer"); // Health display
            }
        }


        return new WirePacket(PacketIds.OBJECTIVE.getPacketId(VersionUtil.SERVER_VERSION), output.toByteArray());
    }
}
