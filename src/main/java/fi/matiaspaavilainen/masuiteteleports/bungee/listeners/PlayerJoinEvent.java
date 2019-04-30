package fi.matiaspaavilainen.masuiteteleports.bungee.listeners;

import fi.matiaspaavilainen.masuitecore.bungee.Utils;
import fi.matiaspaavilainen.masuitecore.core.channels.BungeePluginChannel;
import fi.matiaspaavilainen.masuitecore.core.configuration.BungeeConfiguration;
import fi.matiaspaavilainen.masuitecore.core.objects.MaSuitePlayer;
import fi.matiaspaavilainen.masuiteteleports.bungee.MaSuiteTeleports;
import fi.matiaspaavilainen.masuiteteleports.bungee.commands.SpawnCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class PlayerJoinEvent implements Listener {

    private MaSuiteTeleports plugin;
    private BungeeConfiguration config = new BungeeConfiguration();
    private Utils utils = new Utils();

    public PlayerJoinEvent(MaSuiteTeleports plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConnect(ServerSwitchEvent e) {
        if (e.getPlayer() == null) return;
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            new BungeePluginChannel(plugin, e.getPlayer().getServer().getInfo(), new Object[]{
                    "MaSuiteTeleports",
                    "Invu",
                    e.getPlayer().getName()
            }).send();
        }, 500, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        if (config.load("teleports", "settings.yml").getBoolean("enable-first-spawn") && new MaSuitePlayer().find(e.getPlayer().getUniqueId()).getUniqueId() == null) {
            ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                if (utils.isOnline(e.getPlayer())) {
                    new SpawnCommand(plugin).spawn(e.getPlayer(), 1);
                }
            }, 500, TimeUnit.MILLISECONDS);
        } else if (config.load("teleports", "settings.yml").getBoolean("spawn-on-join")) {
            ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                if (utils.isOnline(e.getPlayer())) {
                    new SpawnCommand(plugin).spawn(e.getPlayer(), 0);
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }


}
