package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdPreventSpawn extends WBCmd {

	public CmdPreventSpawn() {
		name = permission = "preventmobspawn";
		minParams = 0;
		maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - stop mob spawning past border.");
		helpText = "Default value: off. When enabled, this setting will prevent mobs from naturally spawning outside the world's border.";
	}
	
	@Override
	public void cmdStatus(CommandSender sender)
	{
		sender.sendMessage(C_HEAD + "Prevention of mob spawning outside the border is " + enabledColored(Config.preventMobSpawn()) + C_HEAD + ".");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (params.size() == 1) {
			boolean previousSetting = Config.preventMobSpawn();
			Config.setPreventMobSpawn(strAsBool(params.get(0)));
			if (previousSetting != Config.preventMobSpawn()) {
				WorldBorder.plugin.enableMobSpawnListener(Config.preventMobSpawn());
			}
		}

		if (player != null)
		{
			Config.log((Config.preventMobSpawn() ? "Enabled" : "Disabled") + " preventmobspawn at the command of player \"" + player.getName() + "\".");
			cmdStatus(sender);
		}
	}
}
