package org.newhacker1746.sessionskipv3;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;

public class SessionSkipCommand implements SimpleCommand {
    private final SessionSkip plugin;

    public SessionSkipCommand(SessionSkip plugin) {
        this.plugin = plugin;
    }

    /**
   * BrigadierCommand would allow using .requires()
   * But to implement permissions with SimpleCommand must
   * override since default is to just return true (Velocity src)
   */

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("sessionskip.admin");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            usage(src);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                String result1 = plugin.reloadConfig(src);
                if (src instanceof Player) {
                    src.sendMessage(Component.text(result1));
                }
                break;
            case "disable":
                String result2 = plugin.setEnabled(false, src);
                if (src instanceof Player) {
                    src.sendMessage(Component.text(result2));
                }
                break;
            case "enable":
                String result3 = plugin.setEnabled(true, src);
                if (src instanceof Player) {
                    src.sendMessage(Component.text(result3));
                }
                break;
            default:
                usage(src);
        }
    }

    private void usage(CommandSource src) {
        src.sendMessage(Component.text("[SessionSkip] Usage:"));
        src.sendMessage(Component.text("/sessionskip reload"));
        src.sendMessage(Component.text("/sessionskip enable"));
        src.sendMessage(Component.text("/sessionskip disable"));
    }
}