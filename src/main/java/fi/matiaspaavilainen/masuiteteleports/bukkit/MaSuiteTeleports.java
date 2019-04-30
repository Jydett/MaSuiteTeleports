package fi.matiaspaavilainen.masuiteteleports.bukkit;

import fi.matiaspaavilainen.masuitecore.bukkit.chat.Formator;
import fi.matiaspaavilainen.masuitecore.bukkit.commands.PlayerTabCompleter;
import fi.matiaspaavilainen.masuitecore.core.Updator;
import fi.matiaspaavilainen.masuitecore.core.channels.BukkitPluginChannel;
import fi.matiaspaavilainen.masuitecore.core.configuration.BukkitConfiguration;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.BackCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.force.TpAllCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.force.TpCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.force.TpHereCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.force.TpToggleCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.requests.*;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.spawns.SpawnDeleteCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.spawns.SpawnSetCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.commands.spawns.SpawnTeleportCommand;
import fi.matiaspaavilainen.masuiteteleports.bukkit.listeners.TeleportListener;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaSuiteTeleports extends JavaPlugin implements Listener {

    public BukkitConfiguration config = new BukkitConfiguration();
    public Formator formator = new Formator();

    public final List<CommandSender> in_command = new ArrayList<>();
    public List<UUID> tpQue = new ArrayList<>();
    public static List<Player> ignoreTeleport = new ArrayList<>();


    @Override
    public void onEnable() {
        // Create configs
        config.create(this, "teleports", "config.yml");
        config.create(this, "teleports", "messages.yml");
        config.create(this, "teleports", "syntax.yml");

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new TeleportListener(this));

        // Load commands
        loadCommands();
        loadTabCompleters();
        new Updator(new String[]{getDescription().getVersion(), getDescription().getName(), "60125"}).checkUpdates();
    }

    private void loadCommands() {
        // Force
        getCommand("tpall").setExecutor(new TpAllCommand(this));
        getCommand("tphere").setExecutor(new TpHereCommand(this));
        getCommand("tp").setExecutor(new TpCommand(this));
        getCommand("tptoggle").setExecutor(new TpToggleCommand(this));

        // Requests
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this));
        getCommand("tpahere").setExecutor(new TpaHere(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpalock").setExecutor(new TpaLockCommand(this));

        // Spawn
        getCommand("delspawn").setExecutor(new SpawnDeleteCommand(this));
        getCommand("setspawn").setExecutor(new SpawnSetCommand(this));
        getCommand("spawn").setExecutor(new SpawnTeleportCommand(this));

        // Back
        getCommand("back").setExecutor(new BackCommand(this));
    }


    private void loadTabCompleters() {
        getCommand("tpall").setTabCompleter(new PlayerTabCompleter(1));
        getCommand("tphere").setTabCompleter(new PlayerTabCompleter(1));
        getCommand("tp").setTabCompleter(new PlayerTabCompleter(1));

        getCommand("tpa").setTabCompleter(new PlayerTabCompleter(1));
        getCommand("tpahere").setTabCompleter(new PlayerTabCompleter(1));
    }

    @EventHandler
    public void onDeath(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        switch (config.load("teleports", "config.yml").getString("respawn-type").toLowerCase()) {
            case ("none"):
                break;
            case ("bed"):
                if (p.getBedSpawnLocation() != null) {
                    p.teleport(p.getBedSpawnLocation());
                } else {
                    new BukkitPluginChannel(this, p, new Object[]{"MaSuiteTeleports", "SpawnPlayer", p.getName()}).send();
                }
                break;
            case ("home"):
                new BukkitPluginChannel(this, p, new Object[]{"MaSuiteTeleports", "HomeCommand", p.getName(), "home"}).send();
                break;
            case ("spawn"):
                if (p.hasPermission("masuiteleports.spawn.teleport.first")) {
                    new BukkitPluginChannel(this, p, new Object[]{"MaSuiteTeleports", "FirstSpawnPlayer", p.getName()}).send();
                } else {
                    new BukkitPluginChannel(this, p, new Object[]{"MaSuiteTeleports", "SpawnPlayer", p.getName()}).send();
                }

                break;

        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Location loc = e.getEntity().getLocation();
        new BukkitPluginChannel(this, e.getEntity(), new Object[]{"MaSuiteTeleports", "GetLocation", e.getEntity().getName(), loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch()}).send();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (tpQue.contains(e.getEntity().getUniqueId())) {
                e.setCancelled(true);
            }
        }

    }
    
    @EventHandler (ignoreCancelled = true)
    public void playerTeleport(PlayerTeleportEvent e) {

        //Ignore non-players and no command or plugins reasons
        if (e.getPlayer() instanceof Player && (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || e.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) && !e.getPlayer().hasMetadata("NPC")) {

            if(ignoreTeleport.contains(e.getPlayer())) {
                ignoreTeleport.remove(e.getPlayer());
                return;
            }

            Location loc = e.getPlayer().getLocation();
            new BukkitPluginChannel(this, e.getPlayer(), new Object[]{"MaSuiteTeleports", "GetLocation", e.getPlayer().getName(), loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch()}).send();
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        in_command.remove(e.getPlayer());

        ignoreTeleport.remove(e.getPlayer());

        Location loc = e.getPlayer().getLocation();
        new BukkitPluginChannel(this, e.getPlayer(), new Object[]{"MaSuiteTeleports", "GetLocation", e.getPlayer().getName(), loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch()}).send();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        // Prevent save back location accross servers on the destination server
        ignoreTeleport.add(e.getPlayer());
        getServer().getScheduler().runTaskLaterAsynchronously( this, () -> ignoreTeleport.remove(e.getPlayer()), 20 );

        if (getConfig().getBoolean("spawn.first")) {
            if (!e.getPlayer().hasPlayedBefore()) {
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> new BukkitPluginChannel(this, e.getPlayer(), new Object[]{"MaSuiteTeleports", "FirstSpawnPlayer", e.getPlayer().getName()}).send(), 10);
            }
        }
    }
}
