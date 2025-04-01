package ljsure.cn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class StopServerNotice extends JavaPlugin implements CommandExecutor, TabCompleter {

    private BukkitTask countdownTask;
    private final AtomicInteger remainingSeconds = new AtomicInteger(0);

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("sstop")).setExecutor(this);
        Objects.requireNonNull(getCommand("sstop")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("sstop.use")) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }

        // 处理取消命令
        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            String cancelReason = parseReason(args, 1);
            cancelCountdown();
            Bukkit.broadcastMessage("§a服务器关闭倒计时已取消！" +
                    (cancelReason.isEmpty() ? "" : " 原因：" + cancelReason));
            return true;
        }

        // 解析秒数和原因
        int seconds = 60;
        int reasonIndex = 0;

        if (args.length > 0) {
            try {
                seconds = Integer.parseInt(args[0]);
                reasonIndex = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c请输入有效的正整秒数！");
                return false;
            }
        }

        // 默认关闭原因
        String shutdownReason = parseReason(args, reasonIndex);
        startCountdown(seconds, shutdownReason);
        return true;
    }

    private String parseReason(String[] args, int startIndex) {
        if (args.length <= startIndex) return "";
        StringBuilder reason = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }
        return reason.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, @NotNull String alias, String @NotNull [] args) {
        if (!sender.hasPermission("sstop.use")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        // 第一个参数补全
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("cancel".startsWith(input)) {
                completions.add("cancel");
            }
        }
        // 第二个及以上参数不补全
        return completions;
    }

    private void startCountdown(int seconds, String reason) {
        // 取消已有倒计时
        if (countdownTask != null) {
            Bukkit.getScheduler().cancelTask(countdownTask.getTaskId());
        }

        // 广播关闭原因
        if (!reason.isEmpty()) {
            Bukkit.broadcastMessage("§6服务器关闭原因：§f" + reason);
        }

        remainingSeconds.set(seconds);

        countdownTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int current = remainingSeconds.getAndDecrement();

            if (current <= 0) {
                Bukkit.broadcastMessage("§c服务器正在关闭...");
                Bukkit.getScheduler().scheduleSyncDelayedTask(this,
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop"));
                countdownTask.cancel();
                return;
            }

            String message = "§e服务器将在 §c" + current + " 秒§e后关闭" ;

//            String message = "§e服务器将在 §c" + current + " 秒§e后关闭" +
//                    (reason.isEmpty() ? "" : " §7(原因：" + reason + ")");

            if (current % 10 == 0 || current <= 5) {
                Bukkit.broadcastMessage(message);
            }

            // 最后5秒特殊提示
            if (current <= 5) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("§c" + current, "§e服务器即将关闭", 0, 40, 10);
                }
            }
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        remainingSeconds.set(0);
    }

    @Override
    public void onDisable() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }
}