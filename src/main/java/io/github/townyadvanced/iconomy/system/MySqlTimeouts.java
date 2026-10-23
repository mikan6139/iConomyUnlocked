package io.github.townyadvanced.iconomy.system;

import java.util.regex.Pattern;

final class MySqlTimeouts {

	private MySqlTimeouts() {}

	static String applyDefaults(String url, int connectTimeout, int socketTimeout) {
		url = appendIfAbsent(url, "connectTimeout", Math.max(1, connectTimeout));
		return appendIfAbsent(url, "socketTimeout", Math.max(1, socketTimeout));
	}

	private static String appendIfAbsent(String url, String name, int value) {
		if (Pattern.compile("[?&]" + name + "=", Pattern.CASE_INSENSITIVE).matcher(url).find())
			return url;
		String separator = url.endsWith("?") || url.endsWith("&") ? "" : url.contains("?") ? "&" : "?";
		return url + separator + name + "=" + value;
	}
}
