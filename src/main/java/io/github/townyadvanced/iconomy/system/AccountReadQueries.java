package io.github.townyadvanced.iconomy.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public final class AccountReadQueries {

	private final Supplier<Connection> connections;
	private final String table;

	public AccountReadQueries(BackEnd backend, String table) {
		this(backend::getConnection, table);
	}

	AccountReadQueries(Supplier<Connection> connections, String table) {
		this.connections = connections;
		this.table = table;
	}

	private Connection connect() throws SQLException {
		Connection connection = connections.get();
		if (connection == null)
			throw new SQLException("No database connection available");
		return connection;
	}

	public LinkedHashMap<String, Double> ranking(int amount) throws SQLException {
		LinkedHashMap<String, Double> ranking = new LinkedHashMap<>();
		try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
				"SELECT username,balance FROM " + table
				+ " WHERE (hidden = FALSE OR hidden IS NULL) ORDER BY balance DESC LIMIT ?")) {
			statement.setInt(1, amount);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next())
					ranking.put(rows.getString("username"), rows.getDouble("balance"));
			}
		}
		return ranking;
	}

	public Stats stats() throws SQLException {
		try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
				"SELECT COUNT(*), COALESCE(SUM(balance), 0) FROM " + table);
				ResultSet rows = statement.executeQuery()) {
			if (!rows.next())
				throw new SQLException("Account statistics query returned no row");
			return new Stats(rows.getLong(1), rows.getDouble(2));
		}
	}

	public record Stats(long accounts, double total) {
		public double average() {
			return accounts == 0 ? 0.0D : total / accounts;
		}
	}
}
