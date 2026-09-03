package io.github.townyadvanced.iconomy.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import io.github.townyadvanced.iconomy.iConomyUnlocked;
import io.github.townyadvanced.iconomy.system.Account;

public class PlayerJoinListener implements Listener {

	private static final int MAX_ATTEMPTS = 5;
	private static final long RETRY_DELAY_TICKS = 40L; // 2 seconds

	/**
	 * Listens to the PlayerJoinEvent in order to create new Accounts for players
	 * who have not logged in.
	 *
	 * @param event PlayerJoinEvent we listen to.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		attemptAccountCreation(player.getUniqueId(), player.getName(), 1);
	}

	/**
	 * Fetches/creates the joining player's account, retrying a few times on
	 * failure before giving up.
	 *
	 * @param uuid    the joining player's UUID.
	 * @param name    the joining player's name.
	 * @param attempt the current attempt number (1-indexed).
	 */
	private void attemptAccountCreation(java.util.UUID uuid, String name, int attempt) {
		Account account = iConomyUnlocked.getAccounts().get(uuid, name, true);
		if (account != null)
			return;

		if (attempt >= MAX_ATTEMPTS) {
			iConomyUnlocked.getPlugin().getLogger().warning(
				"Error creating / grabbing account for: " + name + " after " + attempt + " attempts. "
				+ "They will not have a balance and will be missing from /money top until this succeeds.");
			return;
		}

		iConomyUnlocked.getPlugin().getLogger().warning(
			"Failed to create / grab account for: " + name + " (attempt " + attempt + "/" + MAX_ATTEMPTS
			+ "). Retrying in " + (RETRY_DELAY_TICKS / 20) + "s...");

		iConomyUnlocked.getPlugin().getServer().getScheduler().runTaskLater(
			iConomyUnlocked.getPlugin(),
			() -> attemptAccountCreation(uuid, name, attempt + 1),
			RETRY_DELAY_TICKS);
	}
}
