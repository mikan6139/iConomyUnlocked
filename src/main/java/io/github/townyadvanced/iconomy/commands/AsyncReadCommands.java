package io.github.townyadvanced.iconomy.commands;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.townyadvanced.iconomy.settings.LangStrings;
import io.github.townyadvanced.iconomy.settings.Settings;
import io.github.townyadvanced.iconomy.system.AccountReadQueries;
import io.github.townyadvanced.iconomy.util.AsyncReadExecutor;
import io.github.townyadvanced.iconomy.util.Messaging;
import io.github.townyadvanced.iconomy.util.Permissions;

public final class AsyncReadCommands implements AutoCloseable {

	private final JavaPlugin plugin;
	private final AccountReadQueries queries;
	private final AsyncReadExecutor executor = new AsyncReadExecutor();
	private final boolean folia;

	public AsyncReadCommands(JavaPlugin plugin, AccountReadQueries queries) {
		this.plugin = plugin;
		this.queries = queries;
		boolean detected;
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			detected = true;
		} catch (ClassNotFoundException ignored) {
			detected = false;
		}
		folia = detected;
	}

	public void showTop(CommandSender sender, int amount) {
		submit(sender, "iConomy.list", () -> queries.ranking(amount), ranking -> {
			Messaging.send(sender, LangStrings.topHeader(String.valueOf(amount)));
			if (ranking.isEmpty()) {
				Messaging.send(sender, LangStrings.topEmpty());
				return;
			}
			int count = 1;
			for (Map.Entry<String, Double> account : ranking.entrySet())
				Messaging.send(sender, LangStrings.topLine(String.valueOf(count++), account.getKey(), Settings.format(account.getValue())));
		});
	}

	public void stats(CommandSender sender) {
		submit(sender, "iConomy.admin.stats", queries::stats, stats -> {
			Messaging.send(sender, LangStrings.statsHeader());
			Messaging.send(sender, LangStrings.statsTotal(Settings.getCurrencyName(), Settings.format(stats.total())));
			Messaging.send(sender, LangStrings.statsAverage(Settings.getCurrencyName(), Settings.format(stats.average())));
			Messaging.send(sender, LangStrings.statsAccounts(String.valueOf(stats.accounts())));
		});
	}

	private <T> void submit(CommandSender sender, String permission, Callable<T> query, Consumer<T> reply) {
		if (!Permissions.hasPermission(sender, permission, true))
			return;
		Object key = sender instanceof Player ? ((Player) sender).getUniqueId() : sender;
		boolean accepted = executor.submit(key, query,
			(task, retired) -> dispatch(sender, task, retired),
			result -> {
				if (Permissions.hasPermission(sender, permission, true))
					reply.accept(result);
			}, error -> {
				plugin.getLogger().log(Level.WARNING, "Failed to read economy statistics.", error);
				Messaging.sendErrorMessage(sender, "Could not fetch account data. Please try again later.");
			});
		if (!accepted)
			Messaging.sendErrorMessage(sender, "An account query is pending or the database is busy. Please wait.");
	}

	private void dispatch(CommandSender sender, Runnable task, Runnable retired) {
		if (!plugin.isEnabled()) {
			retired.run();
			return;
		}
		Runnable delivery = () -> {
			if (!plugin.isEnabled() || (sender instanceof Player && !((Player) sender).isOnline()))
				retired.run();
			else
				task.run();
		};
		if (!folia) {
			Bukkit.getScheduler().runTask(plugin, delivery);
			return;
		}
		try {
			if (sender instanceof Player) {
				Method getter = Player.class.getMethod("getScheduler");
				Object scheduler = getter.invoke(sender);
				boolean scheduled = (Boolean) getter.getReturnType()
					.getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class)
					.invoke(scheduler, plugin, delivery, retired, 1L);
				if (!scheduled)
					retired.run();
			} else {
				Method getter = Bukkit.class.getMethod("getGlobalRegionScheduler");
				Object scheduler = getter.invoke(null);
				getter.getReturnType().getMethod("execute", Plugin.class, Runnable.class)
					.invoke(scheduler, plugin, delivery);
			}
		} catch (ReflectiveOperationException error) {
			retired.run();
			if (plugin.isEnabled())
				plugin.getLogger().log(Level.WARNING, "Could not schedule the economy query reply.", error);
		}
	}

	@Override
	public void close() {
		executor.close();
	}
}
