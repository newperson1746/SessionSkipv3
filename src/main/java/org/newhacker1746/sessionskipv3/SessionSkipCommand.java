package org.newhacker1746.sessionskipv3;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
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
                String resultMsg = plugin.reloadConfig();
                if (!(src instanceof ConsoleCommandSource)) {
                    src.sendMessage(Component.text(resultMsg));
                }
                break;
            case "disable":
                plugin.setEnabled(false);
                src.sendMessage(Component.text("[SessionSkip] Plugin disabled (effective until restart)."));
                break;
            case "enable":
                plugin.setEnabled(true);
                src.sendMessage(Component.text("[SessionSkip] Plugin enabled (effective until restart)."));
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