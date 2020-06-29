package pw.biome.netherblockbreakcounter;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pro.husk.ichat.iChat;
import pro.husk.ichat.obj.PlayerCache;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class NetherBlockBreakCounter extends JavaPlugin implements Listener {

    private final ConcurrentHashMap<UUID, Integer> scores = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        iChat.getPlugin().stopScoreboardTask();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::saveToFileTask, 600, 600);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::updateScoreboards, 10, 10);
    }

    @Override
    public void onDisable() {
        saveToFileTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkOrLoadUserStats(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Block block = event.getBlock();

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (block.getLocation().getWorld().getName().equalsIgnoreCase("world_nether")) {
                int currentScore = scores.get(uuid);
                scores.put(uuid, currentScore + 1);
            }
        });
    }

    private void saveToFileTask() {
        CompletableFuture.runAsync(() -> scores.keySet().forEach(key -> {
            int score = scores.get(key);
            getConfig().set(key.toString(), score);
            saveConfig();
        }));
    }

    public void updateScoreboards() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ImmutableList<Player> playerList = ImmutableList.copyOf(getServer().getOnlinePlayers());
            for (Player player : playerList) {
                PlayerCache playerCache = PlayerCache.getFromUUID(player.getUniqueId());

                if (playerCache == null) return;

                int score = scores.getOrDefault(player.getUniqueId(), 0);

                player.setPlayerListHeader(ChatColor.BLUE + "Biome");
                player.setPlayerListName(playerCache.getRank().getPrefix() + player.getDisplayName() + ChatColor.GOLD + " | " + score);
            }
        });
    }

    private void checkOrLoadUserStats(UUID uuid) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (scores.get(uuid) == null) {
                scores.put(uuid, getConfig().getInt(uuid.toString()));
            }
        });
    }
}