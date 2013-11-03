package com.wimbli.WorldBorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.World;


public class WBCommand implements CommandExecutor
{
    private WorldBorder plugin;

	// color values for strings
	private final String clrCmd = ChatColor.AQUA.toString();		// main commands
	private final String clrReq = ChatColor.GREEN.toString();		// required values
	private final String clrOpt = ChatColor.DARK_GREEN.toString();	// optional values
	private final String clrDesc = ChatColor.WHITE.toString();		// command descriptions
	private final String clrHead = ChatColor.YELLOW.toString();		// command listing header
	private final String clrErr = ChatColor.RED.toString();			// errors / notices

	public WBCommand (WorldBorder plugin)
	{
        this.plugin = plugin;
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split)
	{
		Player player = (sender instanceof Player) ? (Player)sender : null;

		String cmd = clrCmd + ((player == null) ? "wb" : "/wb");
		String cmdW = clrCmd + ((player == null) ? "wb " + clrReq + "<world>" : "/wb " + clrOpt + "[world]") + clrCmd;

		// if world name is passed inside quotation marks, handle that
		if (split.length > 2 && split[0].startsWith("\""))
		{
			if (split[0].endsWith("\""))
			{
				split[0] = split[0].substring(1, split[0].length() - 1);
			}
			else
			{
				List<String> args = new ArrayList<String>();
				String quote = split[0];
				int loop;
				for (loop = 1; loop < split.length; loop++)
				{
					quote += " " + split[loop];
					if (split[loop].endsWith("\""))
						break;
				}

				if (loop < split.length || !split[loop].endsWith("\""))
				{
					args.add(quote.substring(1, quote.length() - 1));
					loop++;
					while (loop < split.length)
					{
						args.add(split[loop]);
						loop++;
					}
					split = args.toArray(new String[0]);
				}
			}
		}

		// "set" command from player or console, world specified
		if ((split.length == 5 || split.length == 6) && split[1].equalsIgnoreCase("set"))
		{
			if (!Config.HasPermission(player, "set")) return true;

			World world = sender.getServer().getWorld(split[0]);
			if (world == null)
				sender.sendMessage("The world you specified (\"" + split[0] + "\") could not be found on the server, but data for it will be stored anyway.");

			if(cmdSet(sender, split[0], split, 2, (split.length == 5)) && player != null)
				sender.sendMessage("Border has been set. " + Config.BorderDescription(split[0]));
		}

		// "set" command from player, using current world, X and Z specified
		else if ((split.length == 4 || split.length == 5) && split[0].equalsIgnoreCase("set") && player != null)
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = player.getWorld().getName();

			if (cmdSet(sender, world, split, 1, (split.length == 4)))
				sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "set" command from player, using current world, X and Z NOT specified
		else if ((split.length == 2 || split.length == 3) && split[0].equalsIgnoreCase("set") && player != null)
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = player.getWorld().getName();

			double x = player.getLocation().getX();
			double z = player.getLocation().getZ();
			int radiusX;
			int radiusZ;
			try
			{
				radiusX = Integer.parseInt(split[1]);
				if (split.length == 3)
					radiusZ = Integer.parseInt(split[2]);
				else
					radiusZ = radiusX;
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The radius value(s) must be integers.");
				return true;
			}

			Config.setBorder(world, radiusX, radiusZ, x, z);
			sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "setcorners" command from player or console, world specified
		else if (split.length == 6 && split[1].equalsIgnoreCase("setcorners"))
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = split[0];
			World worldTest = sender.getServer().getWorld(world);
			if (worldTest == null)
				sender.sendMessage("The world you specified (\"" + world + "\") could not be found on the server, but data for it will be stored anyway.");

			try
			{
				double x1 = Double.parseDouble(split[2]);
				double z1 = Double.parseDouble(split[3]);
				double x2 = Double.parseDouble(split[4]);
				double z2 = Double.parseDouble(split[5]);
				Config.setBorderCorners(world, x1, z1, x2, z2);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The x1, z1, x2, and z2 values must be numerical.");
				return true;
			}

			if(player != null)
				sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "setcorners" command from player, using current world
		else if (split.length == 5 && split[0].equalsIgnoreCase("setcorners") && player != null)
		{
			if (!Config.HasPermission(player, "set")) return true;

			String world = player.getWorld().getName();

			try
			{
				double x1 = Double.parseDouble(split[1]);
				double z1 = Double.parseDouble(split[2]);
				double x2 = Double.parseDouble(split[3]);
				double z2 = Double.parseDouble(split[4]);
				Config.setBorderCorners(world, x1, z1, x2, z2);
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The x1, z1, x2, and z2 values must be numerical.");
				return true;
			}

			sender.sendMessage("Border has been set. " + Config.BorderDescription(world));
		}

		// "radius" command from player or console, world specified
		else if ((split.length == 3 || split.length == 4) && split[1].equalsIgnoreCase("radius"))
		{
			if (!Config.HasPermission(player, "radius")) return true;

			String world = split[0];

			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage(clrErr + "That world (\"" + world + "\") must first have a border set normally.");
				return true;
			}

			double x = border.getX();
			double z = border.getZ();
			int radiusX;
			int radiusZ;
			try
			{
				radiusX = Integer.parseInt(split[2]);
				if (split.length == 4)
					radiusZ = Integer.parseInt(split[3]);
				else
					radiusZ = radiusX;
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The radius value(s) must be integers.");
				return true;
			}

			Config.setBorder(world, radiusX, radiusZ, x, z);

			if (player != null)
				sender.sendMessage("Radius has been set. " + Config.BorderDescription(world));
		}

		// "radius" command from player, using current world
		else if ((split.length == 2 || split.length == 3) && split[0].equalsIgnoreCase("radius") && player != null)
		{
			if (!Config.HasPermission(player, "radius")) return true;

			String world = player.getWorld().getName();

			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage(clrErr + "This world (\"" + world + "\") must first have a border set normally.");
				return true;
			}

			double x = border.getX();
			double z = border.getZ();
			int radiusX;
			int radiusZ;
			try
			{
				radiusX = Integer.parseInt(split[1]);
				if (split.length == 3)
					radiusZ = Integer.parseInt(split[2]);
				else
					radiusZ = radiusX;
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The radius value(s) must be integers.");
				return true;
			}

			Config.setBorder(world, radiusX, radiusZ, x, z);
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
				sender.sendMessage(clrErr + "Your current world (\"" + world + "\") does not have a border set.");
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

			sender.sendMessage("Default border shape for all worlds is \"" + Config.ShapeName() + "\".");

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

			if (split[1].equalsIgnoreCase("rectangular") || split[1].equalsIgnoreCase("square"))
				Config.setShape(false);
			else if (split[1].equalsIgnoreCase("elliptic") || split[1].equalsIgnoreCase("round"))
				Config.setShape(true);
			else
			{
				sender.sendMessage("You must specify a shape of \"elliptic\"/\"round\" or \"rectangular\"/\"square\".");
				return true;
			}

			if (player != null)
				sender.sendMessage("Default border shape for all worlds is now set to \"" + Config.ShapeName() + "\".");
		}

		// "getmsg" command from player or console
		else if (split.length == 1 && split[0].equalsIgnoreCase("getmsg"))
		{
			if (!Config.HasPermission(player, "getmsg")) return true;

			sender.sendMessage("Border message is currently set to:");
			sender.sendMessage(Config.MessageRaw());
			sender.sendMessage("Formatted border message:");
			sender.sendMessage(Config.Message());
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
				sender.sendMessage(Config.MessageRaw());
				sender.sendMessage("Formatted border message:");
				sender.sendMessage(Config.Message());
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

			Config.setDebug(strAsBool(split[1]));

			if (player != null)
				Config.Log((Config.Debug() ? "Enabling" : "Disabling") + " debug output at the command of player \"" + player.getName() + "\".");

			if (player != null)
				sender.sendMessage("Debug mode " + enabledColored(Config.Debug()) + ".");
		}

		// "whoosh" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("whoosh"))
		{
			if (!Config.HasPermission(player, "whoosh")) return true;

			Config.setWhooshEffect(strAsBool(split[1]));

			if (player != null)
			{
				Config.Log((Config.whooshEffect() ? "Enabling" : "Disabling") + " \"whoosh\" knockback effect at the command of player \"" + player.getName() + "\".");
				sender.sendMessage("\"Whoosh\" knockback effect " + enabledColored(Config.whooshEffect()) + ".");
			}
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
				sender.sendMessage(clrErr + "The knockback must be a decimal value of at least 1.0, or it can be 0.");
				return true;
			}

			if (numBlocks < 0.0 || (numBlocks > 0.0 && numBlocks < 1.0))
			{
				sender.sendMessage(clrErr + "The knockback must be a decimal value of at least 1.0, or it can be 0.");
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
				sender.sendMessage(clrErr + "The timer delay must be an integer of 1 or higher.");
				return true;
			}
			if (delay < 1)
			{
				sender.sendMessage(clrErr + "The timer delay must be an integer of 1 or higher.");
				return true;
			}

			Config.setTimerTicks(delay);

			if (player != null)
				sender.sendMessage("Timer delay set to " + delay + " tick(s). That is roughly " + (delay * 50) + "ms.");
		}

		// "wshape" command from player or console, world specified
		else if (split.length == 3 && split[0].equalsIgnoreCase("wshape"))
		{
			if (!Config.HasPermission(player, "wshape")) return true;

			String world = split[1];
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage("The world you specified (\"" + world + "\") does not have a border set.");
				return true;
			}

			Boolean shape = null;
			if (split[2].equalsIgnoreCase("rectangular") || split[2].equalsIgnoreCase("square"))
				shape = false;
			else if (split[2].equalsIgnoreCase("elliptic") || split[2].equalsIgnoreCase("round"))
				shape = true;

			border.setShape(shape);
			Config.setBorder(world, border);

			if (player != null)
				sender.sendMessage("Border shape for world \"" + world + "\" is now set to \"" + (shape == null ? "default" : Config.ShapeName(shape.booleanValue())) + "\".");
		}

		// "wshape" command from player, using current world
		else if (split.length == 2 && split[0].equalsIgnoreCase("wshape") && player != null)
		{
			if (!Config.HasPermission(player, "wshape")) return true;

			String world = player.getWorld().getName();
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage("This world (\"" + world + "\") does not have a border set.");
				return true;
			}

			Boolean shape = null;
			if (split[1].equalsIgnoreCase("rectangular") || split[1].equalsIgnoreCase("square"))
				shape = false;
			else if (split[1].equalsIgnoreCase("elliptic") || split[1].equalsIgnoreCase("round"))
				shape = true;

			border.setShape(shape);
			Config.setBorder(world, border);

			sender.sendMessage("Border shape for world \"" + world + "\" is now set to \"" + (shape == null ? "default" : Config.ShapeName(shape.booleanValue())) + "\".");
		}

		// "wrap" command from player or console, world specified
		else if (split.length == 3 && split[0].equalsIgnoreCase("wrap"))
		{
			if (!Config.HasPermission(player, "wrap")) return true;

			String world = split[1];
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage("The world you specified (\"" + world + "\") does not have a border set.");
				return true;
			}

			boolean wrap = strAsBool(split[2]);
			border.setWrapping(wrap);
			Config.setBorder(world, border);

			if (player != null)
				sender.sendMessage("Border for world \"" + world + "\" is now set to " + (wrap ? "" : "not ") + "wrap around.");
		}

		// "wrap" command from player, using current world
		else if (split.length == 2 && split[0].equalsIgnoreCase("wrap") && player != null)
		{
			if (!Config.HasPermission(player, "wrap")) return true;

			String world = player.getWorld().getName();
			BorderData border = Config.Border(world);
			if (border == null)
			{
				sender.sendMessage("This world (\"" + world + "\") does not have a border set.");
				return true;
			}

			boolean wrap = strAsBool(split[1]);
			border.setWrapping(wrap);
			Config.setBorder(world, border);

			sender.sendMessage("Border for world \"" + world + "\" is now set to " + (wrap ? "" : "not ") + "wrap around.");
		}

		// "portal" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("portal"))
		{
			if (!Config.HasPermission(player, "portal")) return true;

			Config.setPortalRedirection(strAsBool(split[1]));

			if (player != null)
			{
				Config.Log((Config.portalRedirection() ? "Enabling" : "Disabling") + " portal redirection at the command of player \"" + player.getName() + "\".");
				sender.sendMessage("Portal redirection " + enabledColored(Config.portalRedirection()) + ".");
			}
		}

		// "fill" command from player or console, world specified
		else if (split.length >= 2 && split[1].equalsIgnoreCase("fill"))
		{
			if (!Config.HasPermission(player, "fill")) return true;

			boolean cancel = false, confirm = false, pause = false;
			String frequency = "";
			if (split.length >= 3)
			{
				cancel = split[2].equalsIgnoreCase("cancel") || split[2].equalsIgnoreCase("stop");
				confirm = split[2].equalsIgnoreCase("confirm");
				pause = split[2].equalsIgnoreCase("pause");
				if (!cancel && !confirm && !pause)
					frequency = split[2];
			}
			String pad = (split.length >= 4) ? split[3] : "";
			String forceLoad = (split.length >= 5) ? split[4] : "";

			String world = split[0];

			cmdFill(sender, player, world, confirm, cancel, pause, pad, frequency, forceLoad);
		}

		// "fill" command from player (or from console solely if using cancel or confirm), using current world
		else if (split.length >= 1 && split[0].equalsIgnoreCase("fill"))
		{
			if (!Config.HasPermission(player, "fill")) return true;

			boolean cancel = false, confirm = false, pause = false;
			String frequency = "";
			if (split.length >= 2)
			{
				cancel = split[1].equalsIgnoreCase("cancel") || split[1].equalsIgnoreCase("stop");
				confirm = split[1].equalsIgnoreCase("confirm");
				pause = split[1].equalsIgnoreCase("pause");
				if (!cancel && !confirm && !pause)
					frequency = split[1];
			}
			String pad = (split.length >= 3) ? split[2] : "";
			String forceLoad = (split.length >= 4) ? split[3] : "";

			String world = "";
			if (player != null && !cancel && !confirm && !pause)
				world = player.getWorld().getName();

			if (!cancel && !confirm && !pause && world.isEmpty())
			{
				sender.sendMessage("You must specify a world! Example: " + cmdW + " fill " + clrOpt + "[freq] [pad] [force]");
				return true;
			}

			cmdFill(sender, player, world, confirm, cancel, pause, pad, frequency, forceLoad);
		}

		// "trim" command from player or console, world specified
		else if (split.length >= 2 && split[1].equalsIgnoreCase("trim"))
		{
			if (!Config.HasPermission(player, "trim")) return true;

			boolean cancel = false, confirm = false, pause = false;
			String pad = "", frequency = "";
			if (split.length >= 3)
			{
				cancel = split[2].equalsIgnoreCase("cancel") || split[2].equalsIgnoreCase("stop");
				confirm = split[2].equalsIgnoreCase("confirm");
				pause = split[2].equalsIgnoreCase("pause");
				if (!cancel && !confirm && !pause)
					frequency = split[2];
			}
			if (split.length >= 4)
				pad = split[3];

			String world = split[0];

			cmdTrim(sender, player, world, confirm, cancel, pause, pad, frequency);
		}

		// "trim" command from player (or from console solely if using cancel or confirm), using current world
		else if (split.length >= 1 && split[0].equalsIgnoreCase("trim"))
		{
			if (!Config.HasPermission(player, "trim")) return true;

			boolean cancel = false, confirm = false, pause = false;
			String pad = "", frequency = "";
			if (split.length >= 2)
			{
				cancel = split[1].equalsIgnoreCase("cancel") || split[1].equalsIgnoreCase("stop");
				confirm = split[1].equalsIgnoreCase("confirm");
				pause = split[1].equalsIgnoreCase("pause");
				if (!cancel && !confirm && !pause)
					frequency = split[1];
			}
			if (split.length >= 3)
				pad = split[2];

			String world = "";
			if (player != null && !cancel && !confirm && !pause)
				world = player.getWorld().getName();

			if (!cancel && !confirm && !pause && world.isEmpty())
			{
				sender.sendMessage("You must specify a world! Example: " + cmdW+" trim " + clrOpt + "[freq] [pad]");
				return true;
			}

			cmdTrim(sender, player, world, confirm, cancel, pause, pad, frequency);
		}

		// "remount" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("remount"))
		{
			if (!Config.HasPermission(player, "remount")) return true;

			int delay = 0;
			try
			{
				delay = Integer.parseInt(split[1]);
				if (delay < 0)
					throw new NumberFormatException();
			}
			catch(NumberFormatException ex)
			{
				sender.sendMessage(clrErr + "The remount delay must be an integer of 0 or higher. Setting to 0 will disable remounting.");
				return true;
			}

			Config.setRemountTicks(delay);

			if (player != null)
			{
				if (delay == 0)
					sender.sendMessage("Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
				else
				{
					sender.sendMessage("Remount delay set to " + delay + " tick(s). That is roughly " + (delay * 50) + "ms / " + (((double)delay * 50.0) / 1000.0) + " seconds. Setting to 0 would disable remounting.");
					if (delay < 10)
						sender.sendMessage(clrErr + "WARNING:" + clrDesc + " setting this to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
				}
			}
		}

		// "dynmap" command from player or console
		else if (split.length == 2 && split[0].equalsIgnoreCase("dynmap"))
		{
			if (!Config.HasPermission(player, "dynmap")) return true;

			Config.setDynmapBorderEnabled(strAsBool(split[1]));

			sender.sendMessage("DynMap border display " + (Config.DynmapBorderEnabled() ? "enabled" : "disabled") + ".");

			if (player != null)
				Config.Log((Config.DynmapBorderEnabled() ? "Enabled" : "Disabled") + " DynMap border display at the command of player \"" + player.getName() + "\".");
		}

		// "dynmapmsg" command from player or console
		else if (split.length >= 2 && split[0].equalsIgnoreCase("dynmapmsg"))
		{
			if (!Config.HasPermission(player, "dynmapmsg")) return true;

			String message = "";
			for(int i = 1; i < split.length; i++)
			{
				if (i != 1)
					message += ' ';
				message += split[i];
			}

			Config.setDynmapMessage(message);

			if (player != null)
			{
				sender.sendMessage("DynMap border label is now set to:");
				sender.sendMessage(clrErr + Config.DynmapMessage());
			}
		}

		// "bypass" command from player or console, player specified, on/off optionally specified
		else if (split.length >= 2 && split[0].equalsIgnoreCase("bypass"))
		{
			if (!Config.HasPermission(player, "bypass")) return true;

			String sPlayer = split[1];

			boolean bypassing = !Config.isPlayerBypassing(sPlayer);
			if (split.length > 2)
				bypassing = strAsBool(split[2]);

			Config.setPlayerBypass(sPlayer, bypassing);

			Player target = Bukkit.getPlayer(sPlayer);
			if (target != null && target.isOnline())
				target.sendMessage("Border bypass is now " + enabledColored(bypassing) + ".");

			Config.Log("Border bypass for player \"" + sPlayer + "\" is " + (bypassing ? "enabled" : "disabled") + (player != null ? " at the command of player \"" + player.getName() + "\"" : "") + ".");
			if (player != null && player != target)
				sender.sendMessage("Border bypass for player \"" + sPlayer + "\" is " + enabledColored(bypassing) + ".");
		}

		// "bypass" command from player, using them for player
		else if (split.length == 1 && split[0].equalsIgnoreCase("bypass") && player != null)
		{
			if (!Config.HasPermission(player, "bypass")) return true;

			String sPlayer = player.getName();

			boolean bypassing = !Config.isPlayerBypassing(sPlayer);
			Config.setPlayerBypass(sPlayer, bypassing);

			Config.Log("Border bypass is " + (bypassing ? "enabled" : "disabled") + " for player \"" + sPlayer + "\".");
			sender.sendMessage("Border bypass is now " + enabledColored(bypassing) + ".");
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
				if (page > 4)
					page = 1;
			}

			sender.sendMessage(clrHead + plugin.getDescription().getFullName() + " - commands (" + clrReq + "<required> " + clrOpt + "[optional]" + clrHead + ")" + (page > 0 ? " " + page + "/4" : "") + ":");

			if (page == 0 || page == 1)
			{
				if (player != null)
					sender.sendMessage(cmd+" set " + clrReq + "<radiusX> " + clrOpt + "[radiusZ]" + clrDesc + " - set border, centered on you.");
				sender.sendMessage(cmdW+" set " + clrReq + "<radiusX> " + clrOpt + "[radiusZ] <x> <z>" + clrDesc + " - set border.");
				sender.sendMessage(cmdW+" setcorners " + clrReq + "<x1> <z1> <x2> <z2>" + clrDesc + " - set by corners.");
				sender.sendMessage(cmdW+" radius " + clrReq + "<radiusX> " + clrOpt + "[radiusZ]" + clrDesc + " - change radius.");
				sender.sendMessage(cmdW+" clear" + clrDesc + " - remove border for this world.");
				sender.sendMessage(cmd+" clear all" + clrDesc + " - remove border for all worlds.");
				sender.sendMessage(cmd+" shape " + clrReq + "<elliptic|rectangular>" + clrDesc + " - set the default shape.");
				sender.sendMessage(cmd+" shape " + clrReq + "<round|square>" + clrDesc + " - same as above.");
				if (page == 1)
					sender.sendMessage(cmd+" 2" + clrDesc + " - view second page of commands.");
			}
			if (page == 0 || page == 2)
			{
				sender.sendMessage(cmd+" list" + clrDesc + " - show border information for all worlds.");
				sender.sendMessage(cmdW+" fill " + clrOpt + "[freq] [pad] [force]" + clrDesc + " - generate world to border.");
				sender.sendMessage(cmdW+" trim " + clrOpt + "[freq] [pad]" + clrDesc + " - trim world outside of border.");
				sender.sendMessage(cmd+" bypass " + ((player == null) ? clrReq + "<player>" : clrOpt + "[player]") + clrOpt + " [on/off]" + clrDesc + " - let player go beyond border.");
				sender.sendMessage(cmd+" wshape " + ((player == null) ? clrReq + "<world>" : clrOpt + "[world]") + clrReq + " <elliptic|rectangular|default>" + clrDesc + " - shape override for this world.");
				// above command takes 2 lines, so only 7 commands total listed for this page
				sender.sendMessage(cmd+" wshape " + ((player == null) ? clrReq + "<world>" : clrOpt + "[world]") + clrReq + " <round|square|default>" + clrDesc + " - same as above.");
				sender.sendMessage(cmd+" wrap " + ((player == null) ? clrReq + "<world>" : clrOpt + "[world]") + clrReq + " <on/off>" + clrDesc + " - can make border crossings wrap.");
				if (page == 2)
					sender.sendMessage(cmd+" 3" + clrDesc + " - view third page of commands.");
			}
			if (page == 0 || page == 3)
			{
				sender.sendMessage(cmd+" whoosh " + clrReq + "<on|off>" + clrDesc + " - turn knockback effect on or off.");
				sender.sendMessage(cmd+" getmsg" + clrDesc + " - display border message.");
				sender.sendMessage(cmd+" setmsg " + clrReq + "<text>" + clrDesc + " - set border message.");
				sender.sendMessage(cmd+" knockback " + clrReq + "<distance>" + clrDesc + " - how far to move the player back.");
				sender.sendMessage(cmd+" delay " + clrReq + "<amount>" + clrDesc + " - time between border checks.");
				sender.sendMessage(cmd+" remount " + clrReq + "<amount>" + clrDesc + " - player remount delay after knockback.");
				sender.sendMessage(cmd+" dynmap " + clrReq + "<on|off>" + clrDesc + " - turn DynMap border display on or off.");
				sender.sendMessage(cmd+" dynmapmsg " + clrReq + "<text>" + clrDesc + " - DynMap border labels will show this.");
				if (page == 3)
					sender.sendMessage(cmd+" 4" + clrDesc + " - view fourth page of commands.");
			}
			if (page == 0 || page == 4)
			{
				sender.sendMessage(cmd+" portal " + clrReq + "<on|off>" + clrDesc + " - turn portal redirection on or off.");
				sender.sendMessage(cmd+" reload" + clrDesc + " - re-load data from config.yml.");
				sender.sendMessage(cmd+" debug " + clrReq + "<on|off>" + clrDesc + " - turn console debug output on or off.");
				if (page == 4)
					sender.sendMessage(cmd + clrDesc + " - view first page of commands.");
			}
		}

		return true;
	}


	private boolean strAsBool(String str)
	{
		str = str.toLowerCase();
		if (str.startsWith("y") || str.startsWith("t") || str.startsWith("on") || str.startsWith("+") || str.startsWith("1"))
		{
			return true;
		}
		return false;
	}

	private String enabledColored(boolean enabled)
	{
		return enabled ? clrReq+"enabled" : clrErr+"disabled";
	}

	private boolean cmdSet(CommandSender sender, String world, String[] data, int offset, boolean oneRadius)
	{
		int radiusX, radiusZ;
		double x, z;
		try
		{
			radiusX = Integer.parseInt(data[offset]);
			if (oneRadius)
			{
				radiusZ = radiusX;
				offset -= 1;
			}
			else
				radiusZ = Integer.parseInt(data[offset+1]);

			x = Double.parseDouble(data[offset+2]);
			z = Double.parseDouble(data[offset+3]);
		}
		catch(NumberFormatException ex)
		{
			sender.sendMessage(clrErr + "The radius value(s) must be integers and the x and z values must be numerical.");
			return false;
		}

		Config.setBorder(world, radiusX, radiusZ, x, z);
		return true;
	}


	private String fillWorld = "";
	private int fillFrequency = 20;
	private int fillPadding = CoordXZ.chunkToBlock(13);
	private boolean fillForceLoad = false;

	private void fillDefaults()
	{
		fillWorld = "";
		fillFrequency = 20;
		// with "view-distance=10" in server.properties and "Render Distance: Far" in client, hitting border during testing
		// was loading 11 chunks beyond the border in a couple of directions (10 chunks in the other two directions); thus:
		fillPadding = CoordXZ.chunkToBlock(13);
		fillForceLoad = false;
	}

	private boolean cmdFill(CommandSender sender, Player player, String world, boolean confirm, boolean cancel, boolean pause, String pad, String frequency, String forceLoad)
	{
		if (cancel)
		{
			sender.sendMessage(clrHead + "Cancelling the world map generation task.");
			fillDefaults();
			Config.StopFillTask();
			return true;
		}

		if (pause)
		{
			if (Config.fillTask == null || !Config.fillTask.valid())
			{
				sender.sendMessage(clrHead + "The world map generation task is not currently running.");
				return true;
			}
			Config.fillTask.pause();
			sender.sendMessage(clrHead + "The world map generation task is now " + (Config.fillTask.isPaused() ? "" : "un") + "paused.");
			return true;
		}

		if (Config.fillTask != null && Config.fillTask.valid())
		{
			sender.sendMessage(clrHead + "The world map generation task is already running.");
			return true;
		}

		// set padding and/or delay if those were specified
		try
		{
			if (!pad.isEmpty())
				fillPadding = Math.abs(Integer.parseInt(pad));
			if (!frequency.isEmpty())
				fillFrequency = Math.abs(Integer.parseInt(frequency));
		}
		catch(NumberFormatException ex)
		{
			sender.sendMessage(clrErr + "The frequency and padding values must be integers.");
			return false;
		}
		if (!forceLoad.isEmpty())
			fillForceLoad = strAsBool(forceLoad);

		// set world if it was specified
		if (!world.isEmpty())
			fillWorld = world;

		if (confirm)
		{	// command confirmed, go ahead with it
			if (fillWorld.isEmpty())
			{
				sender.sendMessage(clrErr + "You must first use this command successfully without confirming.");
				return false;
			}

			if (player != null)
				Config.Log("Filling out world to border at the command of player \"" + player.getName() + "\".");

			int ticks = 1, repeats = 1;
			if (fillFrequency > 20)
				repeats = fillFrequency / 20;
			else
				ticks = 20 / fillFrequency;
				
			Config.fillTask = new WorldFillTask(plugin.getServer(), player, fillWorld, fillPadding, repeats, ticks, fillForceLoad);
			if (Config.fillTask.valid())
			{
				int task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, Config.fillTask, ticks, ticks);
				Config.fillTask.setTaskID(task);
				sender.sendMessage("WorldBorder map generation for world \"" + fillWorld + "\" task started.");
			}
			else
				sender.sendMessage(clrErr + "The world map generation task failed to start.");

			fillDefaults();
		}
		else
		{
			if (fillWorld.isEmpty())
			{
				sender.sendMessage(clrErr + "You must first specify a valid world.");
				return false;
			}

			String cmd = clrCmd + ((player == null) ? "wb" : "/wb");
			sender.sendMessage(clrHead + "World generation task is ready for world \"" + fillWorld + "\", padding the map out to " + fillPadding + " blocks beyond the border (default " + CoordXZ.chunkToBlock(13) + "), and the task will try to generate up to " + fillFrequency + " chunks per second (default 20). Parts of the world which are already fully generated will be " + (fillForceLoad ? "loaded anyway." : "skipped."));
			sender.sendMessage(clrHead + "This process can take a very long time depending on the world's border size. Also, depending on the chunk processing rate, players will likely experience severe lag for the duration.");
			sender.sendMessage(clrDesc + "You should now use " + cmd + " fill confirm" + clrDesc + " to start the process.");
			sender.sendMessage(clrDesc + "You can cancel at any time with " + cmd + " fill cancel" + clrDesc + ", or pause/unpause with " + cmd + " fill pause" + clrDesc + ".");
		}
		return true;
	}


	private String trimWorld = "";
	private int trimFrequency = 5000;
	private int trimPadding = CoordXZ.chunkToBlock(13);

	private void trimDefaults()
	{
		trimWorld = "";
		trimFrequency = 5000;
		trimPadding = CoordXZ.chunkToBlock(13);
	}

	private boolean cmdTrim(CommandSender sender, Player player, String world, boolean confirm, boolean cancel, boolean pause, String pad, String frequency)
	{
		if (cancel)
		{
			sender.sendMessage(clrHead + "Cancelling the world map trimming task.");
			trimDefaults();
			Config.StopTrimTask();
			return true;
		}

		if (pause)
		{
			if (Config.trimTask == null || !Config.trimTask.valid())
			{
				sender.sendMessage(clrHead + "The world map trimming task is not currently running.");
				return true;
			}
			Config.trimTask.pause();
			sender.sendMessage(clrHead + "The world map trimming task is now " + (Config.trimTask.isPaused() ? "" : "un") + "paused.");
			return true;
		}

		if (Config.trimTask != null && Config.trimTask.valid())
		{
			sender.sendMessage(clrHead + "The world map trimming task is already running.");
			return true;
		}

		// set padding and/or delay if those were specified
		try
		{
			if (!pad.isEmpty())
				trimPadding = Math.abs(Integer.parseInt(pad));
			if (!frequency.isEmpty())
				trimFrequency = Math.abs(Integer.parseInt(frequency));
		}
		catch(NumberFormatException ex)
		{
			sender.sendMessage(clrErr + "The frequency and padding values must be integers.");
			return false;
		}

		// set world if it was specified
		if (!world.isEmpty())
			trimWorld = world;

		if (confirm)
		{	// command confirmed, go ahead with it
			if (trimWorld.isEmpty())
			{
				sender.sendMessage(clrErr + "You must first use this command successfully without confirming.");
				return false;
			}

			if (player != null)
				Config.Log("Trimming world beyond border at the command of player \"" + player.getName() + "\".");

			int ticks = 1, repeats = 1;
			if (trimFrequency > 20)
				repeats = trimFrequency / 20;
			else
				ticks = 20 / trimFrequency;
				
			Config.trimTask = new WorldTrimTask(plugin.getServer(), player, trimWorld, trimPadding, repeats);
			if (Config.trimTask.valid())
			{
				int task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, Config.trimTask, ticks, ticks);
				Config.trimTask.setTaskID(task);
				sender.sendMessage("WorldBorder map trimming task for world \"" + trimWorld + "\" started.");
			}
			else
				sender.sendMessage(clrErr + "The world map trimming task failed to start.");

			trimDefaults();
		}
		else
		{
			if (trimWorld.isEmpty())
			{
				sender.sendMessage(clrErr + "You must first specify a valid world.");
				return false;
			}

			String cmd = clrCmd + ((player == null) ? "wb" : "/wb");
			sender.sendMessage(clrHead + "World trimming task is ready for world \"" + trimWorld + "\", trimming the map past " + trimPadding + " blocks beyond the border (default " + CoordXZ.chunkToBlock(13) + "), and the task will try to process up to " + trimFrequency + " chunks per second (default 5000).");
			sender.sendMessage(clrHead + "This process can take a while depending on the world's overall size. Also, depending on the chunk processing rate, players may experience lag for the duration.");
			sender.sendMessage(clrDesc + "You should now use " + cmd + " trim confirm" + clrDesc + " to start the process.");
			sender.sendMessage(clrDesc + "You can cancel at any time with " + cmd + " trim cancel" + clrDesc + ", or pause/unpause with " + cmd + " trim pause" + clrDesc + ".");
		}
		return true;
	}
}