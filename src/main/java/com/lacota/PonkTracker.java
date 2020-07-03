package com.lacota;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PonkTracker extends JavaPlugin implements Listener {
    private Map<UUID, UUID> tracking;
    private Map<UUID, Long> cooldown;

    @Override
    public void onEnable() {
        tracking = new HashMap<>();
        cooldown = new HashMap<>();

        getCommand("track").setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        tracking.clear();
        cooldown.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (command.getName().equalsIgnoreCase("track")) {
            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "To track a player, do /track <player>");
                player.sendMessage(ChatColor.RED + "To stop tracking, do /track cancel");
                return true;
            }

            if (args[0].equalsIgnoreCase("cancel")) {
                if (!tracking.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You aren't tracking anyone!");
                    return true;
                }

                tracking.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "You have stopped tracking.");
            } else {
                if (getServer().getPlayer(args[0]) == null || !getServer().getPlayer(args[0]).isOnline()) {
                    player.sendMessage(ChatColor.RED + args[0] + " isn't online.");
                    return true;
                }

                Player target = getServer().getPlayer(args[0]);
                tracking.put(player.getUniqueId(), target.getUniqueId());

                player.sendMessage(ChatColor.YELLOW + "You are tracking " + target.getName() + ".");
                player.getInventory().addItem(new ItemStack(Material.COMPASS));
                player.setCompassTarget(target.getLocation());

                target.sendMessage(ChatColor.YELLOW + "You are being tracked by " + player.getName() + ". Good luck!");
                target.getInventory().addItem(new ItemStack(Material.COMPASS));
                target.setCompassTarget(player.getLocation());
            }
        }

        return true;
    }

    @EventHandler
    public void onCompassInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (e.hasItem() && e.getItem().getType() == Material.COMPASS) {

            if (tracking.containsKey(player.getUniqueId())) { // Tracking
                if (getServer().getPlayer(tracking.get(player.getUniqueId())) == null || !getServer().getPlayer(tracking.get(player.getUniqueId())).isOnline()) {
                    player.sendMessage(ChatColor.RED + "The player you were tracking isn't online.");
                    tracking.remove(player.getUniqueId());
                    return;
                }

                Player target = getServer().getPlayer(tracking.get(player.getUniqueId()));
                player.setCompassTarget(target.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Compass pointing to " + target.getName());

            } else if (tracking.containsValue(player.getUniqueId())) { // Being tracked
                if (cooldown.containsKey(player.getUniqueId()) && cooldown.get(player.getUniqueId()) > System.currentTimeMillis()) {
                    player.sendMessage(ChatColor.RED + "You are on cooldown for " + getCooldownRemaining(player));
                    return;
                }

                Player tracker = getServer().getPlayer(tracking
                        .entrySet()
                        .stream()
                        .filter(entry -> player.getUniqueId().equals(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .findFirst().get());

                double distance = player.getLocation().distance(tracker.getLocation());
                distance = Math.round(distance * 10) / (double) 10;

                player.sendMessage(ChatColor.RED + tracker.getName() + " is " + distance + " blocks away!");
                player.setCompassTarget(tracker.getLocation());

                cooldown.put(player.getUniqueId(), System.currentTimeMillis() + 180 * 1000);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        tracking.remove(player.getUniqueId());
        cooldown.remove(player.getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        Player player = e.getPlayer();

        tracking.remove(player.getUniqueId());
        cooldown.remove(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        if (tracking.containsKey(player.getUniqueId())) {
            player.getInventory().addItem(new ItemStack(Material.COMPASS));
        }
    }

    private String getCooldownRemaining(Player player) {
        StringBuilder timeRemaining = new StringBuilder();

        long millisRemaining = cooldown.get(player.getUniqueId()) - System.currentTimeMillis();
        double millisToSeconds = millisRemaining / 1000d;

        if (millisToSeconds > 60) {
            int seconds = (int) (millisToSeconds % 60);
            int minutes = (int) (millisToSeconds / 60);

            timeRemaining.append(minutes + " minutes " + seconds + " seconds.");
        } else {
            double rounded = Math.round(millisToSeconds * 10) / 10d;
            timeRemaining.append(rounded + " seconds.");
        }

        return timeRemaining.toString();
    }
}
