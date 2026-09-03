package io.github.townyadvanced.iconomy.system;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcConnectionPool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.townyadvanced.iconomy.iConomyUnlocked;
import io.github.townyadvanced.iconomy.settings.Settings;

public class BackEnd {
	private static final String plugin_dir = iConomyUnlocked.getPlugin().getDataFolder().getPath();
	private JdbcConnectionPool h2pool;
	private HikariDataSource mysqlPool;
	private String SQLTable = Settings.getDBTable();
	private String dsn;
	private String username;
	private String password;
	private final validDBTypes dbType;
	private Logger log = iConomyUnlocked.getPlugin().getLogger();

	private enum validDBTypes {
		H2, MYSQL
	};

	public BackEnd() throws Exception {
		validDBTypes type = validDBTypes.valueOf(Settings.getDBType());
		this.dbType = type;
		switch (type) {
		case H2:
			dsn = "jdbc:h2:./" + plugin_dir + File.separator + Settings.getDBName() + ";AUTO_RECONNECT=TRUE";
			username = "sa";
			password = "sa";
			if (this.h2pool == null)
				this.h2pool = JdbcConnectionPool.create(this.dsn, this.username, this.password);
			break;
		case MYSQL:
			this.dsn = "jdbc:mysql://"
					+ Settings.getMysqlHostname() + ":"
					+ Settings.getMysqlPort() + "/"
					+ Settings.getDBName()
					+ Settings.getMysqlFlags();
			this.username = Settings.getMysqlUser();
			this.password = Settings.getMysqlPass();

			HikariConfig hikariConfig = new HikariConfig();
			hikariConfig.setJdbcUrl(this.dsn);
			hikariConfig.setUsername(this.username);
			hikariConfig.setPassword(this.password);
			hikariConfig.setPoolName("iConomyUnlocked-MySQL");
			hikariConfig.setMaximumPoolSize(Math.max(2, Settings.getMysqlPoolSize()));
			hikariConfig.setMinimumIdle(Math.min(2, hikariConfig.getMaximumPoolSize()));
			hikariConfig.setConnectionTimeout(8000L);
			hikariConfig.setInitializationFailTimeout(-1L);
			hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
			hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
			hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			this.mysqlPool = new HikariDataSource(hikariConfig);
			break;
		default:
			throw new Exception("Unknown DB type set in config.yml: " + Settings.getDBType() + ", no DB connection was established!");
		}
	}

	/**
	 * Create the accounts table if it doesn't exist already.
	 * 
	 * @throws Exception
	 */
	public void setupAccountTable() throws Exception {

		Connection conn = getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		switch (dbType) {
		case H2:
			try {
				ps = conn.prepareStatement(
					"CREATE TABLE " + SQLTable + "("
						+ "id INT auto_increment PRIMARY KEY,"
						+ "uuid VARCHAR(36) UNIQUE,"
						+ "username VARCHAR(32),"
						+ "balance DECIMAL (64, 2),"
						+ "hidden BOOLEAN DEFAULT '0'"
						+ ");"
					);
				ps.executeUpdate();
			} catch (SQLException ignored) {}
			break;

		case MYSQL:
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, SQLTable, null);

			if (!rs.next()) {
				log.info("Creating table: " + SQLTable);
				ps = conn.prepareStatement(
					"CREATE TABLE " + SQLTable + " ("
						+ "`id` INT(10) NOT NULL AUTO_INCREMENT,"
						+ "`uuid` VARCHAR(36) NOT NULL,"
						+ "`username` VARCHAR(32) NOT NULL,"
						+ "`balance` DECIMAL(64, 2) NOT NULL,"
						+ "`hidden` BOOLEAN NOT NULL DEFAULT '0',"
						+ "PRIMARY KEY (`id`),"
						+ "UNIQUE(`uuid`)"
						+ ")"
					);
				if (ps != null) {
					ps.executeUpdate();
					log.info("Table Created.");
				}
			}
			break;

		default:
			break;
		}

		close(conn, ps, rs);
	}

	public void setupTransactionTable() throws Exception {
		if (!Settings.transactionLoggingEnabled())
			return;

		Connection conn = getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		switch (dbType) {
		case H2:
			try {
				ps = conn.prepareStatement(
					"CREATE TABLE " + SQLTable + "_Transactions(" 
							+ "id INT AUTO_INCREMENT PRIMARY KEY, " 
							+ "account_from TEXT, " 
							+ "account_to TEXT, " 
							+ "account_from_balance DECIMAL(64, 2), " 
							+ "account_to_balance DECIMAL(64, 2), " 
							+ "timestamp TEXT, " 
							+ "set DECIMAL(64, 2), " 
							+ "gain DECIMAL(64, 2), " 
							+ "loss DECIMAL(64, 2)" 
							+ ");"
					);

					ps.executeUpdate();
				} catch (SQLException ignored) {}
			break;
		case MYSQL:
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, SQLTable + "_Transactions", null);
			if (!rs.next()) {
				log.info("Creating logging database.. [" + SQLTable + "_Transactions]");
				ps = conn.prepareStatement(
						"CREATE TABLE " + SQLTable + "_Transactions ("
								+ "`id` INT(255) NOT NULL AUTO_INCREMENT, "
								+ "`account_from` TEXT NOT NULL, "
								+ "`account_to` TEXT NOT NULL, "
								+ "`account_from_balance` DECIMAL(65, 2) NOT NULL, " 
								+ "`account_to_balance` DECIMAL(65, 2) NOT NULL, "
								+ "`timestamp` TEXT NOT NULL, "
								+ "`set` DECIMAL(65, 2) NOT NULL, "
								+ "`gain` DECIMAL(65, 2) NOT NULL, "
								+ "`loss` DECIMAL(65, 2) NOT NULL, "
								+ "PRIMARY KEY (`id`)"
								+ ");"
						);
				if (ps != null) {
					ps.executeUpdate();
					log.info("Logging Table Created.");
				}
			}
			break;
		default:
			throw new Exception("Unknown DB type set in config.yml: " + Settings.getDBType() + ", log unable to start!");
		}

		log.info("Logging enabled.");
		close(conn, ps, rs);
	}

	public JdbcConnectionPool connectionPool() {
		return this.h2pool;
	}

	Connection getConnection() {
		try {
			switch (dbType) {
			case H2:
				return this.h2pool.getConnection();
			case MYSQL:
				return this.mysqlPool.getConnection();
			default:
				log.severe("Could not create connection!");
			}

		} catch (SQLException e) {
			log.log(Level.SEVERE, "Could not create a database connection: " + e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Shuts down whichever connection pool is currently in use. Safe to call
	 * regardless of the configured database type.
	 */
	public void close() {
		try {
			if (this.h2pool != null)
				this.h2pool.dispose();
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to dispose of the H2 connection pool cleanly.", e);
		}

		try {
			if (this.mysqlPool != null)
				this.mysqlPool.close();
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to close the MySQL connection pool cleanly.", e);
		}
	}

	void close(Connection conn, PreparedStatement ps, ResultSet rs) {
		if (ps != null)
			try {
				ps.close();
			} catch (SQLException ignored) {}

		if (rs != null)
			try {
				rs.close();
			} catch (SQLException ignored) {}

		if (conn != null)
			try {
				conn.close();
			} catch (SQLException ignored) {}
	}

	void close(Connection conn, PreparedStatement ps) {
		if (ps != null)
			try {
				ps.close();
			} catch (SQLException ignored) {}

		if (conn != null)
			try {
				conn.close();
			} catch (SQLException ignored) {}
	}

	void close(Connection connection) {

		if (connection != null)
			try {
				connection.close();
			} catch (SQLException ignored) {}
	}
}
