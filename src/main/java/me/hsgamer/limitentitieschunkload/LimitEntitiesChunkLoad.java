package me.hsgamer.limitentitieschunkload;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class LimitEntitiesChunkLoad extends JavaPlugin implements Listener {
    private final Queue<Chunk> chunks = new ConcurrentLinkedQueue<>();
    private final AtomicReference<BukkitTask> currentChunkTask = new AtomicReference<>();
    private int maxEntitiesPerChunk = 10;
    private List<String> worlds = new ArrayList<>();
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxEntitiesPerChunk = getConfig().getInt("max-entities-per-chunk", maxEntitiesPerChunk);
        worlds = getConfig().getStringList("worlds");

        Bukkit.getPluginManager().registerEvents(this, this);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (currentChunkTask.get() != null) return;

            Chunk chunk = chunks.poll();
            if (chunk == null) return;
            chunks.add(chunk);

            currentChunkTask.set(Bukkit.getScheduler().runTask(this, () -> {
                int count = 0;
                for (Entity entity : chunk.getEntities()) {
                    if (entity.isValid() && (entity instanceof Item || entity instanceof Monster)) {
                        if (count >= maxEntitiesPerChunk) {
                            entity.remove();
                        }
                        count++;
                    }
                }
                currentChunkTask.set(null);
            }));
        }, 0, 0);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        try {
            BukkitTask chunkTask = currentChunkTask.get();
            if (chunkTask != null) chunkTask.cancel();
        } catch (Exception ignored) {
            // IGNORED
        }
        try {
            if (task != null) task.cancel();
        } catch (Exception ignored) {
            // IGNORED
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        if (worlds.contains(chunk.getWorld().getName())) {
            chunks.add(chunk);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        chunks.remove(event.getChunk());
    }
}
