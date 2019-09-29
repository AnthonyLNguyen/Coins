package me.justeli.coins.events;

import me.jet315.minions.events.MinerBlockBreakEvent;
import me.justeli.coins.api.ActionBar;
import me.justeli.coins.api.Title;
import me.justeli.coins.cancel.PreventSpawner;
import me.justeli.coins.item.Coin;
import me.justeli.coins.main.Coins;
import me.justeli.coins.settings.Config;
import me.justeli.coins.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.Ageable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class DropCoin implements Listener
{
    static Coins c;

    public DropCoin(Coins c){
        DropCoin.c = c;
    }

    @EventHandler
    public void onEntityDamageEvent (EntityDamageEvent e){
        int stackSize = Coins.getStackedAmount((LivingEntity)e.getEntity());
        if (Settings.hB.get(Config.BOOLEAN.debug))
            Coins.console(Coins.LogType.INFO ,"Stack Size: " + stackSize);
        e.getEntity().setMetadata("stackSize", new FixedMetadataValue(c, Coins.getStackedAmount((LivingEntity)e.getEntity())));
    }

	@EventHandler
	public void onDeath (EntityDeathEvent e)
    {
		Entity m = e.getEntity();

        for (String world : Settings.hA.get(Config.ARRAY.disabledWorlds) )
            if (m.getWorld().getName().equalsIgnoreCase(world))
                return;
        if (Coins.usingBeastTokens) {
            if (e.getEntity().getKiller() != null || !Settings.hB.get(Config.BOOLEAN.KillerRequired)) {
                if (m instanceof Player && Settings.hB.get(Config.BOOLEAN.playerDrop) && Coins.getBeastTokens().getTokensManager().getTokens((Player) m) <= 0)
                    return;
                dropTheCoin(m, e.getEntity().getKiller());
            }

            if (m instanceof Player && Settings.hB.get(Config.BOOLEAN.loseOnDeath)) {
                double second = Settings.hD.get(Config.DOUBLE.moneyTaken_from);
                double first = Settings.hD.get(Config.DOUBLE.moneyTaken_to) - second;

                Player p = (Player) e.getEntity();
                double random = Math.random() * first + second;

                Coins.getBeastTokens().getTokensManager().removeTokens(p, (int) random);
                Title.sendSubTitle(p, 20, 100, 20, Settings.hS.get(Config.STRING.deathMessage)
                        .replace("%amount%", String.valueOf((long) random)).replace("{$}", Settings.hS.get(Config.STRING.currencySymbol)));
            }
        } else {
            if (e.getEntity().getKiller() != null) {
                if ((m instanceof Player) && Settings.hB.get(Config.BOOLEAN.playerDrop) && Coins.getEconomy().getBalance((Player) m) <= 0)
                    return;
                dropTheCoin(m, e.getEntity().getKiller());
            }

            if (m instanceof Player && Settings.hB.get(Config.BOOLEAN.loseOnDeath)) {
                double second = Settings.hD.get(Config.DOUBLE.moneyTaken_from);
                double first = Settings.hD.get(Config.DOUBLE.moneyTaken_to) - second;

                Player p = (Player) e.getEntity();
                double random = Math.random() * first + second;

                Coins.getEconomy().withdrawPlayer(p, (long) random);
                Title.sendSubTitle(p, 20, 100, 20, Settings.hS.get(Config.STRING.deathMessage)
                        .replace("%amount%", String.valueOf((long) random)).replace("{$}", Settings.hS.get(Config.STRING.currencySymbol)));
            }
        }
	}

	@EventHandler
    public void onPistonEvent(BlockPistonExtendEvent p){
        List<Block> blockList = p.getBlocks();
        BlockFace dir = p.getDirection();
        Coins.console(Coins.LogType.INFO,dir.toString());

        for (Block b:
             blockList) {
            Coins.console(Coins.LogType.INFO,"Coord: " + b.getLocation().toString() + "->" + b.getLocation().add(dir.getDirection()).toString());
            Coins.console(Coins.LogType.INFO,"Chunk: " + b.getChunk().toString() + "->" + b.getLocation().add(dir.getDirection()).getChunk().toString());
            Coins.blockTracking.addLocation(b.getLocation().add(dir.getDirection()).getChunk().toString(), b.getLocation().add(dir.getDirection()));
            Coins.blockTracking.addLocation(b.getChunk().toString(), b.getLocation());
        }
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent var1) {
        Coins.blockTracking.addLocation(var1.getBlock().getChunk().toString(), var1.getBlock().getLocation());
    }

    @EventHandler
    public void onMinerBlockBreakEvent(MinerBlockBreakEvent b){
        if (Coins.blockTracking.hasLocation(b.getBlock().getChunk().toString(), b.getBlock().getLocation())) {
            Coins.blockTracking.removeLocation(b.getBlock().getChunk().toString(), b.getBlock().getLocation());
            return;
        }
        if (b.getBlock() != null) {
            if (!b.isCancelled()) {
                if (b.getMinion().getPlayer().hasPermission("Coins.CoinBlockDrop")) {
                    String blockWorld = b.getBlock().getLocation().getWorld().getName();
                    String pathWorlds = "DropChances.Worlds." + blockWorld + ".";
                    String pathGlobal = "DropChances.Global.";
                    String path = pathWorlds;
                    Material mat = b.getBlock().getType();
                    int chance = -1;
                    int min = 0;
                    int max = 0;
                    int amount = 1;
                    double roll;
                    for (String world : Settings.hA.get(Config.ARRAY.disabledWorlds))
                        if (b.getBlock().getWorld().getName().equalsIgnoreCase(world))
                            return;
                    if (c.getBlock().isSet(pathWorlds + mat.toString())) {
                        path = pathWorlds;
                    } else if (c.getBlock().isSet(pathGlobal + mat.toString())) {
                        path = pathGlobal;
                    } else if (c.getBlock().isSet(pathWorlds + "ALL")) {
                        path = pathWorlds;
                    } else if (c.getBlock().isSet(pathGlobal + "ALL")) {
                        path = pathGlobal;
                    } else return;
                    chance = c.getBlock().getInt(path + mat + ".Chance");
                    max = c.getBlock().getInt(path + mat + ".Max");
                    min = c.getBlock().getInt(path + mat + ".Min");

                    amount = min + (int)(Math.random() * ((max - min) + 1));

                    Double second = Settings.hD.get(Config.DOUBLE.moneyAmount_from);
                    Double first  = Settings.hD.get(Config.DOUBLE.moneyAmount_to);
                    if (second > first)
                        amount = (int) (amount * ( Math.random() * (first  - second) + second ));

                    roll = Math.random() * 99 + 1;
                    if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Amount: " + amount + " | Chance: " + chance + " | Roll: " + roll + " (" + (chance >= roll) + ").");
                    if (chance >= roll) {
                        if (b.getBlock().getBlockData() instanceof Ageable) {
                            Ageable crop = (Ageable) b.getBlock().getBlockData();
                            if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Ageable broken " + crop.getAge() + ":" + crop.getMaximumAge());
                            if (crop.getAge() < crop.getMaximumAge())
                                return;
                        }
                        if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Drop Block detected: " + b.getBlock().getType().name());
                        if (Coins.usingBeastTokens) {
                            Coins.getBeastTokens().getTokensManager().addTokens(b.getMinion().getPlayer(), amount);
                        } else {
                            Coins.getEconomy().depositPlayer(b.getMinion().getPlayer(), amount);
                        }
                        new ActionBar("&dMinion mined" + Settings.hS.get(Config.STRING.pickupMessage)
                                .replace("+", "")
                                .replace("%amount%", "" + amount)
                                .replace("{$}", Settings.hS.get(Config.STRING.currencySymbol))).send(b.getMinion().getPlayer());
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockBreak(BlockBreakEvent b) {
        if (Coins.blockTracking.hasLocation(b.getBlock().getChunk().toString(), b.getBlock().getLocation())) {
            Coins.blockTracking.removeLocation(b.getBlock().getChunk().toString(), b.getBlock().getLocation());
            return;
        }
        ItemStack tool = b.getPlayer().getItemInHand();
        if (tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            return;
        }
        if (b.getBlock() != null) {
            if (!b.isCancelled()) {
                if (b.getPlayer().hasPermission("Coins.CoinBlockDrop")) {
                    String blockWorld = b.getBlock().getLocation().getWorld().getName();
                    String pathWorlds = "DropChances.Worlds." + blockWorld + ".";
                    String pathGlobal = "DropChances.Global.";
                    String path = pathWorlds;
                    Material mat = b.getBlock().getType();
                    int chance = -1;
                    int min = 0;
                    int max = 0;
                    int amount = 1;
                    double roll;
                    for (String world : Settings.hA.get(Config.ARRAY.disabledWorlds))
                        if (b.getBlock().getWorld().getName().equalsIgnoreCase(world))
                            return;
                    if (c.getBlock().isSet(pathWorlds + mat.toString())) {
                        path = pathWorlds;
                    } else if (c.getBlock().isSet(pathGlobal + mat.toString())) {
                        path = pathGlobal;
                    } else if (c.getBlock().isSet(pathWorlds + "ALL")) {
                        path = pathWorlds;
                    } else if (c.getBlock().isSet(pathGlobal + "ALL")) {
                        path = pathGlobal;
                    } else return;
                    chance = c.getBlock().getInt(path + mat + ".Chance");
                    max = c.getBlock().getInt(path + mat + ".Max");
                    min = c.getBlock().getInt(path + mat + ".Min");

                    amount = min + (int)(Math.random() * ((max - min) + 1));

                    roll = Math.random() * 99 + 1;
                    if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Amount: " + amount + " | Chance: " + chance + " | Roll: " + roll + " (" + (chance >= roll) + ").");
                    if (chance >= roll) {
                        if (b.getBlock().getBlockData() instanceof Ageable) {
                            Ageable crop = (Ageable) b.getBlock().getBlockData();
                            if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Ageable broken " + crop.getAge() + ":" + crop.getMaximumAge());
                            if (crop.getAge() < crop.getMaximumAge())
                                return;
                        }
                        if (Settings.hB.get(Config.BOOLEAN.debug)) Coins.console(Coins.LogType.INFO, "Drop Block detected: " + b.getBlock().getType().name());
                        for (int i = 0; i < amount; i++) {
                            ItemStack coin = new Coin().stack(!Settings.hB.get(Config.BOOLEAN.dropEachCoin)).item();
                            Item ccoin = b.getBlock().getLocation().getWorld().dropItemNaturally(b.getBlock().getLocation(), coin);
                            Entity ecoin = ccoin;
                            if (ccoin.getItemStack().getAmount() == 1)
                                ecoin.setCustomName(ChatColor.translateAlternateColorCodes('&', Settings.hS.get(Config.STRING.nameOfCoin)));
                            else
                                ecoin.setCustomName(ChatColor.translateAlternateColorCodes('&', Settings.hS.get(Config.STRING.nameOfCoin)) + Settings.hS.get(Config.STRING.multiSuffix));
                            ecoin.setCustomNameVisible(true);
                        }
                    }
                }
            }
        }
    }

	private void dropTheCoin (Entity m, Player p) {
        CoinDropEvent dropEvent = new CoinDropEvent(m, p);
        Bukkit.getPluginManager().callEvent(dropEvent);
        if (dropEvent.isCancelled())
            return;

        if (m.getType().equals(EntityType.PLAYER) && Settings.hB.get(Config.BOOLEAN.preventAlts)) {
            Player player = (Player) m;
            if (p.getAddress().getAddress().getHostAddress()
                    .equals(player.getAddress().getAddress().getHostAddress()))
                return;
        }

        boolean stack;
        if (Settings.hB.get(Config.BOOLEAN.dropEachCoin)) stack = false;
        else stack = dropEvent.isStackable();

        if (PreventSpawner.fromSplit(m)) {
            PreventSpawner.removeFromList(m);
            return;
        }
        if (p != null ) {
            if (!PreventSpawner.fromSpawner(m) || p.hasPermission("coins.spawner")) {
                    int amount = 1;
                    int chance = -1;
                    int min = 0;
                    int max = 0;
                    double roll;
                    String entityWorld = m.getLocation().getWorld().getName();
                    EntityType e = m.getType();
                    String pathWorlds = "DropChances.Worlds." + entityWorld + ".";
                    String pathGlobal = "DropChances.Global.";
                    String path = pathWorlds;
                    if (c.getMob().isSet(pathWorlds + e.name())) {
                        path = pathWorlds;
                    } else if (c.getMob().isSet(pathGlobal + e.name())) {
                        path = pathGlobal;
                    } else if (c.getMob().isSet(pathWorlds + "ALL")) {
                        path = pathWorlds;
                    } else if (c.getMob().isSet(pathGlobal + "ALL")) {
                        path = pathGlobal;
                    } else return;
                    chance = c.getMob().getInt(path + e + ".Chance");
                    max = c.getMob().getInt(path + e + ".Max");
                    min = c.getMob().getInt(path + e + ".Min");

                    amount = min + (int)(Math.random() * ((max - min) + 1));
                    if (Settings.hB.get(Config.BOOLEAN.debug))
                        Coins.console(Coins.LogType.INFO,(LivingEntity) m + " | Stack Amount: " + m.getMetadata("stackSize"));
                    List<MetadataValue> list = m.getMetadata("stackSize");
                    amount *= list.get(0).asInt();

                    roll = Math.random() * 99 + 1;
                    if (Settings.hB.get(Config.BOOLEAN.debug))
                        Coins.console(Coins.LogType.INFO, "Amount: " + amount + " | Chance: " + chance + " | Roll: " + roll + " (" + (chance >= roll) + ").");
                    if (chance >= roll) {
                        if (Settings.hB.get(Config.BOOLEAN.debug))
                            Coins.console(Coins.LogType.INFO, "Kill Drop detected: " + e.name());
                        for (int i = 0; i < amount; i++) {
                            ItemStack coin = new Coin().stack(!Settings.hB.get(Config.BOOLEAN.dropEachCoin)).item();
                            Item ccoin = m.getLocation().getWorld().dropItemNaturally(m.getLocation(), coin);
                            Entity ecoin = ccoin;
                            ecoin.setCustomName(ChatColor.translateAlternateColorCodes('&', Settings.hS.get(Config.STRING.nameOfCoin)) + Settings.hS.get(Config.STRING.multiSuffix));
                            ecoin.setCustomNameVisible(true);
                        }
                    }
            } else PreventSpawner.removeFromList(m);
        }

    }
}
