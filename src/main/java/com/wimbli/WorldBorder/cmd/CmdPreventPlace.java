package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;

public class CmdPreventPlace extends WBCmd {

	public CmdPreventPlace() {
		name = permission = "preventblockplace";
		minParams = 0;
		maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - stop block placement past border.");
		helpText = "Default value: off. When enabled, this setting will prevent players from placing blocks outside the world's border.";
	}
	
	@Override
	public void cmdStatus(CommandSender sender)
	{
		sender.sendMessage(C_HEAD + "Prevention of block placement outside the border is " + enabledColored(Config.preventBlockPlace()) + C_HEAD + ".");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (params.size() == 1) {
			boolean previousSetting = Config.preventBlockPlace();
			Config.setPreventBlockPlace(strAsBool(params.get(0)));
			if (previousSetting != Config.preventBlockPlace()) {
				WorldBorder.plugin.enableBlockPlaceListener(Config.preventBlockPlace());
			}
		}

		if (player != null)
		{
			Config.log((Config.preventBlockPlace() ? "Enabled" : "Disabled") + " preventblockplace at the command of player \"" + player.getName() + "\".");
			cmdStatus(sender);
		}
	}
}
