package io.github.townyadvanced.iconomy.system;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import io.github.townyadvanced.iconomy.iConomyUnlocked;
import io.github.townyadvanced.iconomy.events.AccountResetEvent;
import io.github.townyadvanced.iconomy.events.AccountSetEvent;
import io.github.townyadvanced.iconomy.events.AccountUpdateEvent;
import io.github.townyadvanced.iconomy.settings.Settings;

public class Holdings {
	
	private UUID uuid;
    private String name = "";
    private Double balance = null;
    Logger log = iConomyUnlocked.getPlugin().getLogger();
    private String SQLTable = Settings.getDBTable();

    public Holdings(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
    
    /**
     * Holdings name.
     * 
     * @return name of this Holding
     */
    public String getName() {
    	return this.name;
    }
    
    /**
     * Holdings UUID.
     * 
     * @return UUID of this Holding
     */
    public UUID getUUID() {
    	return this.uuid;
    }

    /**
     * Get the balance for this Holding.
     * 
     * @return the balance.
     */
    public double balance() {
        if (balance == null)
            balance = get();
        return balance.doubleValue();
    }

    private synchronized double get() {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        double balance = Double.valueOf(Settings.getDefaultBalance());
        try {
            conn = iConomyUnlocked.getBackEnd().getConnection();
            ps = conn.prepareStatement("SELECT * FROM " + SQLTable + " WHERE username = ? LIMIT 1");
            ps.setString(1, this.name);
            rs = ps.executeQuery();

            if (rs.next())
                balance = rs.getDouble("balance");
        } catch (Exception ex) {
            log.warning("Failed to grab holdings: " + ex);
        } finally {
        	iConomyUnlocked.getBackEnd().close(conn, ps, rs);
        }
        return balance;
    }

    public synchronized void set(double balance) {

		balance = round(balance);
		this.balance = balance;
		Bukkit.getPluginManager().callEvent(new AccountSetEvent(this, balance));

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = iConomyUnlocked.getBackEnd().getConnection();
			ps = conn.prepareStatement("UPDATE " + SQLTable + " SET balance = ? WHERE username = ?");
			ps.setDouble(1, balance);
			ps.setString(2, this.name);
			ps.executeUpdate();

		} catch (Exception ex) {
			log.warning("Failed to set holdings: " + ex);
		} finally {
			iConomyUnlocked.getBackEnd().close(conn, ps);
		}
    }

    public synchronized void add(double amount) {
        double balance = balance();
        double ending = round(balance + amount);

        callEventAndSetHoldings(amount, balance, ending);
    }

    public synchronized void subtract(double amount) {
        double balance = balance();
        double ending = round(balance - amount);

        callEventAndSetHoldings(amount, balance, ending);
    }

    public synchronized void divide(double amount) {
        double balance = balance();
        double ending = round(balance / amount);

        callEventAndSetHoldings(amount, balance, ending);
    }

    public synchronized void multiply(double amount) {
        double balance = balance();
        double ending = round(balance * amount);

        callEventAndSetHoldings(amount, balance, ending);
    }

    /**
     * Rounds a monetary value to 2 decimal places, matching the precision
     * used everywhere else (the balance column and Settings#format).
     */
    private static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

	/**
	 * Reset Holdings to default, if the Event is not cancelled.
	 */
	public void reset() {
		Bukkit.getPluginManager().callEvent(new AccountResetEvent(this));
		set(Settings.getDefaultBalance());
	}

	private void callEventAndSetHoldings(double amount, double previousBalance, double newBalance) {
		Bukkit.getPluginManager().callEvent(new AccountUpdateEvent(this, previousBalance, newBalance, amount));
		set(newBalance);
	}

    /**
     * Is this balance negative?
     * 
     * @return true if negative.
     */
    public boolean isNegative() {
        return get() < 0.0D;
    }

    /**
     * Does this Holding have this amount or more?
     * 
     * @param amount the amount to test for.
     * @return true if the balance is sufficient.
     */
    public boolean hasEnough(double amount) {
        return amount <= get();
    }

    /**
     * Is the balance over the amount?
     * 
     * @param amount the amount to test for.
     * @return true if balance is higher.
     */
    public boolean hasOver(double amount) {
        return amount < get();
    }

    /**
     * Is the balance under the amount?
     * 
     * @param amount the amount to test for.
     * @return true if balance is lower.
     */
    public boolean hasUnder(double amount) {
        return amount > get();
    }

    public String toString() {
        return String.format(Settings.getEconomyFormat(), get());
    }
}
