package in.twizmwaz.cardinal.module.modules.respawn;

import in.twizmwaz.cardinal.GameHandler;
import in.twizmwaz.cardinal.chat.ChatConstant;
import in.twizmwaz.cardinal.event.CardinalSpawnEvent;
import in.twizmwaz.cardinal.match.Match;
import in.twizmwaz.cardinal.module.Module;
import in.twizmwaz.cardinal.module.ModuleCollection;
import in.twizmwaz.cardinal.module.modules.blitz.Blitz;
import in.twizmwaz.cardinal.module.modules.cardinalNotifications.CardinalNotifications;
import in.twizmwaz.cardinal.module.modules.filter.FilterModule;
import in.twizmwaz.cardinal.module.modules.filter.FilterState;
import in.twizmwaz.cardinal.module.modules.spawn.SpawnModule;
import in.twizmwaz.cardinal.module.modules.spectatorTools.SpectatorTools;
import in.twizmwaz.cardinal.module.modules.teamPicker.TeamPicker;
import in.twizmwaz.cardinal.module.modules.titleRespawn.TitleRespawn;
import in.twizmwaz.cardinal.module.modules.tutorial.Tutorial;
import in.twizmwaz.cardinal.settings.Settings;
import in.twizmwaz.cardinal.util.Items;
import in.twizmwaz.cardinal.util.Teams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class RespawnModule implements Module {

    private final Match match;

    protected RespawnModule(Match match) {
        this.match = match;
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onMinecraftRespawn(PlayerRespawnEvent event) {
        CardinalSpawnEvent spawnEvent = new CardinalSpawnEvent(event.getPlayer(), Teams.getTeamByPlayer(event.getPlayer()).orNull());
        Bukkit.getServer().getPluginManager().callEvent(spawnEvent);
        if (!spawnEvent.isCancelled()) {
            event.setRespawnLocation(spawnEvent.getSpawn().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCardinalRespawn(CardinalSpawnEvent event) {
        if (event.getSpawn() == null) {
            if (!match.isRunning()) event.setTeam(Teams.getTeamById("observers").get());
            ModuleCollection<SpawnModule> modules = new ModuleCollection<>();
            for (SpawnModule spawnModule : Teams.getSpawns(event.getTeam())) {
                FilterModule filter = spawnModule.getFilter();
                if (filter == null || filter.evaluate(event.getPlayer()).equals(FilterState.ALLOW))
                    modules.add(spawnModule);
            }
            if (modules.size() > 0) {
                event.setSpawn(modules.getRandom());
            } else {
                event.setCancelled(true);
            }
        }
    }

    public static void giveObserversKit(Player player) {
        Match match = GameHandler.getGameHandler().getMatch();
        if (match.getModules().getModule(TitleRespawn.class).isDeadUUID(player.getUniqueId())) return;
        player.getInventory().setItem(0, Items.createItem(Material.COMPASS, 1, (short) 0, ChatColor.BLUE + "" + ChatColor.BOLD + ChatConstant.UI_COMPASS.getMessage(player.getLocale())));
        player.getInventory().setItem(1, CardinalNotifications.book);
        if (!match.hasEnded() && !(Blitz.matchIsBlitz() && match.isRunning())) {
            player.getInventory().setItem(2, TeamPicker.getTeamPicker(player.getLocale()));
        }
        player.getInventory().setItem(3, Tutorial.getEmerald(player));
        if (player.hasPermission("tnt.defuse")) {
            player.getInventory().setItem(5, Items.createItem(Material.SHEARS, 1, (short) 0, ChatColor.RED + ChatConstant.UI_TNT_DEFUSER.getMessage(player.getLocale())));
        }
        if (player.hasPermission("cardinal.punish.freeze")) {
            player.getInventory().setItem(6, Items.createItem(Material.ICE, 1, (short) 0, ChatColor.AQUA + ChatConstant.UI_FREEZE_ITEM.getMessage(player.getLocale())));
        }
        player.getInventory().setItem(8, SpectatorTools.getSpectatorItem(player.getLocale()));

        player.setFlySpeed(0.1f * Float.parseFloat(Settings.getSettingByName("Speed").getValueByPlayer(player).getValue()));
        if (Settings.getSettingByName("Elytra").getValueByPlayer(player).getValue().equals("on")) {
                player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        }
    }

}
