package com.wimbli.WorldBorder;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.World;


public class WBCommand implements CommandExecutor
{
    private WorldBorder plugin;

	public WBCommand (WorldBorder plugin)
	{
        this.plugin = plugin;
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split)
	{
		Player player = (sender instanceof Player) ? (Player)sender : null;

		String cmd = ChatColor.AQUA + ((player == null) ? "wb" : "/wb");
		String cmdW =  ChatColor.AQUA + ((player == null) ? "wb " + ChatColor.GREEN + "<world>" : "/wb " + ChatColor.DARK_GREEN + "[world]") + ChatColor.AQUA;

		// "set" command from player or console, world specified
		if (split.length == 5 && split[1].equalsIgnoreCase("set"))
		{
			if (!Config.HasPermission(player, "set")) return true;

			World world = sender.getServer().getWorld(split[0]);
			if (world == null)
				sender.sendMessage("The world you specified (\"" + split[0] + "\") could not be found on the server, but data for it will be stored anyway.");

			if(cmdSet(sender, split[0], split, 2) && player != null)
				sender.sendMessage("Border has been set. " + Config.BorderDescription(split[0]));
		}

		// "set" command from player, using current world, X and Z specified
		else if (split.length == 4 && split[0].equalsIgnoreCase("set") && player != null)
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = player.getWorld().getName();

			if (cmdSet(sender, world, split, 1))
				sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "set" command from player, using current world, X and Z NOT specified
		else if (split.length == 2 && split[0].equalsIgnoreCase("set") && player != null)
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = player.getWorld().getName();

			double x = player.getLocation().getX();
			double z = player.getLocation().getZ();
			int radius;
			try
			{
				radius = Integer.parseInt(split[1]);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(ChatColor.RED + "The radius value must be an integer.");
				return true;
			}

			Config.setBorder(world, radius, x, z);
			sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "radius" command from player or console, world specified
		else if (split.length == 3 && split[1].equalsIgnoreCase("radius"))
		{
			if (!Config.HasPermission(player, "radius")) return true;

			String world = split[0];

			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage(ChatColor.RED + "That world (\"" + world + "\") must first have a border set normally.");
				return true;
			}

			double x = border.getX();
			double z = border.getZ();
			int radius;
			try
			{
				radius = Integer.parseInt(split[2]);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(ChatColor.RED + "The radius value must be an integer.");
				return true;
			}

			Config.setBorder(world, radius, x, z);

			if (player != null)
				sender.sendMessage("Radius has been set. " + Config.BorderDescription(world));
		}

		// "radius" command from player, using current world
		else if (split.length == 2 && split[0].equalsIgnoreCase("radius") && player != null)
		{
			if (!Config.HasPermission(player, "radius")) return true;

			String world = player.getWorld().getName();

			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage(ChatColor.RED + "This world (\"" + world + "\") must first have a border set normally.");
				return true;
			}

			double x = border.getX();
			double z = border.getZ();
			int radius;
			try
			{
				radius = Integer.parseInt(split[1]);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(ChatColor.RED + "The radius value must be an integer.");
				return true;
			}

			Config.setBorder(world, radius, x, z);
			sender.sendMessage("Radius has been set. " + Config.BorderDescription(world));
		}

		// "clear" command from player or console, world specified
		else if (split.length == 2 && split[1].equalsIgnoreCase("clear"))
		{
			if (!Config.HasPermission(player, "clear")) return true;

			String world = split[0];
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage("The world you specified (\"" + world + "\") does not have a border set.");
				return true;
			}

			Config.removeBorder(world);

			if (player != null)
				sender.sendMessage("Border cleared for world \"" + world + "\".");
		}

		// "clear" command from player, using current world
		else if (split.length == 1 && split[0].equalsIgnoreCase("clear") && player != null)
		{
			if (!Config.HasPermission(player, "clear")) return true;

			String world = player.getWorld().getName();
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage(ChatColor.RED + "Your current world (\"" + world + "\") does not have a border set.");
				return true;
			}

			Config.removeBorder(world);
			sender.sendMessage("Border cleared for world \"" + world + "\".");
		}

		// "clear all" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("clear") && split[1].equalsIgnoreCase("all"))
		{
			if (!Config.HasPermission(player, "clear")) return true;

			Config.removeAllBorders();

			if (player != null)
				sender.sendMessage("All borders cleared for all worlds.");
		}

		// "list" command from player or console
		else if (split.length == 1 && split[0].equalsIgnoreCase("list"))
		{
			if (!Config.HasPermission(player, "list")) return true;

			sender.sendMessage("Border shape for all worlds is \"" + (Config.ShapeRound() ? "round" : "square") + "\".");

			Set<String> list = Config.BorderDescriptions();

			if (list.isEmpty())
			{
				sender.sendMessage("There are no borders currently set.");
				return true;
			}

			Iterator listItem = list.iterator();
			while(listItem.hasNext())
			{
				sender.sendMessage( (String)listItem.next() );
			}
		}

		// "shape" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("shape"))
		{
			if (!Config.HasPermission(player, "shape")) return true;

			if (split[1].equalsIgnoreCase("square"))
				Config.setShape(false);
			else if (split[1].equalsIgnoreCase("round"))
				Config.setShape(true);
			else
			{
				sender.sendMessage("You must specify a shape of \"round\" or \"square\".");
				return true;
			}

			if (player != null)
				sender.sendMessage("Border shape for all worlds is now set to \"" + (Config.ShapeRound() ? "round" : "square") + "\".");
		}

		// "getmsg" command from player or console
		else if (split.length == 1 && split[0].equalsIgnoreCase("getmsg"))
		{
			if (!Config.HasPermission(player, "getmsg")) return true;

			sender.sendMessage("Border message is currently set to:");
			sender.sendMessage(ChatColor.RED + Config.Message());
		}

		// "setmsg" command from player or console
		else if (split.length >= 2 && split[0].equalsIgnoreCase("setmsg"))
		{
			if (!Config.HasPermission(player, "setmsg")) return true;

			String message = "";
			for(int i = 1; i < split.length; i++)
			{
				if (i != 1)
					message += ' ';
				message += split[i];
			}

			Config.setMessage(message);

			if (player != null)
			{
				sender.sendMessage("Border message is now set to:");
				sender.sendMessage(ChatColor.RED + Config.Message());
			}
		}

		// "reload" command from player or console
		else if (split.length == 1 && split[0].equalsIgnoreCase("reload"))
		{
			if (!Config.HasPermission(player, "reload")) return true;

			if (player != null)
				Config.Log("Reloading config file at the command of player \"" + player.getName() + "\".");

			Config.load(plugin, true);

			if (player != null)
				sender.sendMessage("WorldBorder configuration reloaded.");
		}

		// "debug" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("debug"))
		{
			if (!Config.HasPermission(player, "debug")) return true;

			Config.setDebug(split[1].equalsIgnoreCase("on"));

			if (player != null)
				Config.Log((Config.Debug() ? "Enabling" : "Disabling") + " debug output at the command of player \"" + player.getName() + "\".");

			if (player != null)
				sender.sendMessage("Debug mode " + (Config.Debug() ? "enabled" : "disabled") + ".");
		}

		// "knockback" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("knockback"))
		{
			if (!Config.HasPermission(player, "knockback")) return true;

			double numBlocks = 0.0;
			try
			{
				numBlocks = Double.parseDouble(split[1]);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(ChatColor.RED + "The knockback must be a decimal value above 0.");
				return true;
			}

			if (numBlocks <= 0.0)
			{
				sender.sendMessage(ChatColor.RED + "The knockback must be a decimal value above 0.");
				return true;
			}

			Config.setKnockBack(numBlocks);

			if (player != null)
				sender.sendMessage("Knockback set to " + numBlocks + " blocks inside the border.");
		}

		// "delay" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("delay"))
		{
			if (!Config.HasPermission(player, "delay")) return true;

			int delay = 0;
			try
			{
				delay = Integer.parseInt(split[1]);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(ChatColor.RED + "The timer delay must be an integer of 1 or higher.");
				return true;
			}
			if (delay < 1)
			{
				sender.sendMessage(ChatColor.RED + "The timer delay must be an integer of 1 or higher.");
				return true;
			}

			Config.setTimerTicks(delay);

			if (player != null)
				sender.sendMessage("Timer delay set to " + delay + " tick(s). That is roughly " + (delay * 50) + "ms.");
		}

		// we couldn't decipher any known commands, so show help
		else
		{
			if (!Config.HasPermission(player, "help")) return true;

			int page = (player == null) ? 0 : 1;
			if (split.length == 1)
			{
				try
				{
					page = Integer.parseInt(split[0]);
				}
				catch(NumberFormatException ex)
				{
				}
				if (page > 2)
					page = 1;
			}

			sender.sendMessage(ChatColor.YELLOW + plugin.getDescription().getFullName() + " - commands (" + (player != null ? ChatColor.DARK_GREEN + "[optional] " : "") + ChatColor.GREEN + "<required>" + ChatColor.YELLOW + ")" + (page > 0 ? " " + page + "/2" : "") + ":");

			if (page == 0 || page == 1)
			{
				if (player != null)
					sender.sendMessage(cmd+" set " + ChatColor.GREEN + "<radius>" + ChatColor.WHITE + " - set world border, centered on you.");
				sender.sendMessage(cmdW+" set " + ChatColor.GREEN + "<radius> <x> <z>" + ChatColor.WHITE + " - set world border.");
				sender.sendMessage(cmdW+" radius " + ChatColor.GREEN + "<radius>" + ChatColor.WHITE + " - change a border radius.");
				sender.sendMessage(cmdW+" clear" + ChatColor.WHITE + " - remove border for this world.");
				sender.sendMessage(cmd+" clear all" + ChatColor.WHITE + " - remove border for all worlds.");
				sender.sendMessage(cmd+" list" + ChatColor.WHITE + " - show border information for all worlds.");
				sender.sendMessage(cmd+" shape " + ChatColor.GREEN + "<round|square>" + ChatColor.WHITE + " - set the border shape.");
				sender.sendMessage(cmd+" knockback " + ChatColor.GREEN + "<distance>" + ChatColor.WHITE + " - how far to move the player back.");
				if (page == 1)
					sender.sendMessage(cmd+" 2" + ChatColor.WHITE + " - view second page of commands.");
			}

			if (page == 0 || page == 2)
			{
				sender.sendMessage(cmd+" getmsg" + ChatColor.WHITE + " - display border message.");
				sender.sendMessage(cmd+" setmsg " + ChatColor.GREEN + "<text>" + ChatColor.WHITE + " - set border message.");
				sender.sendMessage(cmd+" delay " + ChatColor.GREEN + "<amount>" + ChatColor.WHITE + " - time between border checks.");
				sender.sendMessage(cmd+" reload" + ChatColor.WHITE + " - re-load data from config.yml.");
				sender.sendMessage(cmd+" debug " + ChatColor.GREEN + "<on|off>" + ChatColor.WHITE + " - turn console debug output on or off.");
				if (page == 2)
					sender.sendMessage(cmd + ChatColor.WHITE + " - view first page of commands.");
			}
		}

		return true;
	}


	private boolean cmdSet(CommandSender sender, String world, String[] data, int offset)
	{
		int radius;
		double x, z;
		try
		{
			radius = Integer.parseInt(data[offset]);
			x = Double.parseDouble(data[offset+1]);
			z = Double.parseDouble(data[offset+2]);
		}
		catch(NumberFormatException ex)
		{
			sender.sendMessage(ChatColor.RED + "The radius value must be an integer and the x and z values must be numerical.");
			return false;
		}

		Config.setBorder(world, radius, x, z);
		return true;
	}
}