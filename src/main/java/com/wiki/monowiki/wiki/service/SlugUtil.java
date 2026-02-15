package com.wiki.monowiki.wiki.service;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {}

    public static String slugify(String input) {
	String s = Normalizer.normalize(input, Normalizer.Form.NFD)
		.replaceAll("\\p{M}", "")
		.toLowerCase(Locale.ROOT)
		.replaceAll("[^a-z0-9\\s-]", "")
		.trim()
		.replaceAll("\\s+", "-")
		.replaceAll("-{2,}", "-");

	if (s.isBlank()) s = "article";
	if (s.length() > 140) s = s.substring(0, 140).replaceAll("-+$", "");
	return s;
    }
}
