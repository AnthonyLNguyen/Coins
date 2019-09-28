package me.justeli.coins.events;

import me.justeli.coins.api.Title;
import me.justeli.coins.cancel.PreventSpawner;
import me.justeli.coins.item.Coin;
import me.justeli.coins.main.Coins;
import me.justeli.coins.settings.Config;
import me.justeli.coins.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.Ageable;

public class DropCoin implements Listener
{
	@EventHandler
	public void onDeath (EntityDeathEvent e)
    {
		Entity m = e.getEntity();

        for (String world : Settings.hA.get(Config.ARRAY.disabledWorlds) )
            if (m.getWorld().getName().equalsIgnoreCase(world))
                return;
        if (Settings.hB.get(Config.BOOLEAN.BeastTokens)) {
            if (e.getEntity().getKiller() != null || !Settings.hB.get(Config.BOOLEAN.KillerRequired)) {
                if (
                        (m instanceof Monster || m instanceof Slime || m instanceof Ghast || m instanceof EnderDragon ||
                                (!Settings.hB.get(Config.BOOLEAN.olderServer) && m instanceof Shulker) ||
                                (Settings.hB.get(Config.BOOLEAN.newerServer) && m instanceof Phantom)
                        )

                                || ((m instanceof Animals || m instanceof Squid || m instanceof Snowman || m instanceof IronGolem
                                || m instanceof Villager || m instanceof Ambient) && Settings.hB.get(Config.BOOLEAN.passiveDrop))

                                || (m instanceof Player && Settings.hB.get(Config.BOOLEAN.playerDrop) && Coins.getBeastTokens().getTokensManager().getTokens((Player) m) >= 0)
                        )

                {
                    dropTheCoin(m, e.getEntity().getKiller());
                }

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
                if (
                        (m instanceof Monster || m instanceof Slime || m instanceof Ghast || m instanceof EnderDragon ||
                                (!Settings.hB.get(Config.BOOLEAN.olderServer) && m instanceof Shulker) ||
                                (Settings.hB.get(Config.BOOLEAN.newerServer) && m instanceof Phantom)
                        )

                                || ((m instanceof Animals || m instanceof Squid || m instanceof Snowman || m instanceof IronGolem
                                || m instanceof Villager || m instanceof Ambient) && Settings.hB.get(Config.BOOLEAN.passiveDrop))

                                || (m instanceof Player && Settings.hB.get(Config.BOOLEAN.playerDrop) && Coins.getEconomy().getBalance((Player) m) >= 0)
                        )

                {
                    dropTheCoin(m, e.getEntity().getKiller());
                }

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
    public void onPlayerBreakBlock(BlockBreakEvent b){
        for (String world : Settings.hA.get(Config.ARRAY.disabledWorlds) )
            if (b.getPlayer().getWorld().getName().equalsIgnoreCase(world))
                return;
        if (Settings.hB.get(Config.BOOLEAN.BeastTokens)) {
            if (Settings.multiplier_blocks.containsKey(b.getBlock().getType()) ) {
                int amount = 1;
                amount = Settings.multiplier_blocks.get(b.getBlock().getType());
                int second = Settings.hD.get(Config.DOUBLE.moneyAmount_from).intValue();
                int first = Settings.hD.get(Config.DOUBLE.moneyAmount_to).intValue() + 1 - second;
                amount *= (Math.random() * first + second);
                if (b.getBlock().getBlockData() instanceof Ageable) {
                    Ageable crop = (Ageable) b.getBlock().getBlockData();
                    if (crop.getAge() == crop.getMaximumAge())
                        return;
                }
                Coins.console(Coins.LogType.INFO, "Drop Block detected: " + b.getBlock().getType().name());
                for (int i = 0; i < amount; i++) {
                    ItemStack coin = new Coin().stack(!Settings.hB.get(Config.BOOLEAN.dropEachCoin)).item();
                    b.getBlock().getLocation().getWorld().dropItem(b.getBlock().getLocation(), coin);
                }
            }
        }
    }

	private void dropTheCoin (Entity m, Player p)
    {
        CoinDropEvent dropEvent = new CoinDropEvent(m, p);
        Bukkit.getPluginManager().callEvent(dropEvent);
        if (dropEvent.isCancelled())
            return;

        if (m.getType().equals(EntityType.PLAYER) && Settings.hB.get(Config.BOOLEAN.preventAlts))
        {
            Player player = (Player) m;
            if (p.getAddress().getAddress().getHostAddress()
                    .equals(player.getAddress().getAddress().getHostAddress()))
                return;
        }

        boolean stack;
        if (Settings.hB.get(Config.BOOLEAN.dropEachCoin)) stack = false;
        else stack = dropEvent.isStackable();

        if (PreventSpawner.fromSplit(m))
        {
            PreventSpawner.removeFromList(m);
            return;
        }
        if ( p != null ) {
            if (!PreventSpawner.fromSpawner(m) || p.hasPermission("coins.spawner")) {
                if (Math.random() <= Settings.hD.get(Config.DOUBLE.dropChance)) {
                    int amount = 1;
                    if (Settings.multiplier_mobs.containsKey(m.getType()))
                        amount = Settings.multiplier_mobs.get(m.getType());

                    if (Settings.hB.get(Config.BOOLEAN.dropEachCoin)) {
                        int second = Settings.hD.get(Config.DOUBLE.moneyAmount_from).intValue();
                        int first = Settings.hD.get(Config.DOUBLE.moneyAmount_to).intValue() + 1 - second;

                        amount *= (Math.random() * first + second);
                    }

                    for (int i = 0; i < amount; i++) {
                            ItemStack coin = new Coin().stack(stack).item();
                            m.getLocation().getWorld().dropItem(m.getLocation(), coin);
                    }

                }
            }
        }
        else PreventSpawner.removeFromList(m);
    }

}
