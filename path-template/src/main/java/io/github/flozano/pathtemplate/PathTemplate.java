package io.github.flozano.pathtemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.api.pathtemplate.ValidationException;

public final class PathTemplate {
	private static final CharSequence SLASH_CODE = "%2F";
	private final com.google.api.pathtemplate.PathTemplate engine;
	private final String prefix;
	private final Map<String, List<String>> varsWhichNeedRepeatedProcessing;

	public PathTemplate(String template) {
		if (template.startsWith("//")) {
			throw new IllegalArgumentException("Template cannot start with '//':" + template);
		}
		if (template.endsWith("//")) {
			throw new IllegalArgumentException("Template cannot end in '//': " + template);
		}
		if (template.endsWith("/")) {
			throw new IllegalArgumentException("Template cannot end with '/': " + template);
		}

		if (template.startsWith("/")) {
			this.prefix = "/";
		} else {
			this.prefix = "";
		}
		engine = com.google.api.pathtemplate.PathTemplate.createWithoutUrlEncoding(template);
		// ensure no var contains two modifiers
		engine.vars().stream() //
				.filter(v -> occurrencesOfCharGreaterThan(v, '?', 1)) //
				.findAny() //
				.ifPresent(v -> {
					throw new IllegalArgumentException("Variable cannot contain two modifiers: " + v);
				});
		// Check for repeated variables
		this.varsWhichNeedRepeatedProcessing = engine.vars().stream() //
				.filter(v -> v.contains("#"))    //
				.map(v -> v.contains("?") ? v.split("\\?")[0] : v) //
				.collect(Collectors.groupingBy(v -> v.split("#")[0], Collectors.toList()));
	}

	public static String render(String template, Map<String, String> variables) {
		return new PathTemplate(template).render(variables);
	}

	public String render(Map<String, String> variables) {
		Map<String, String> processed = new HashMap<>(Objects.requireNonNull(variables));
		// Process the values and the repeated
		for (String key : variables.keySet()) {
			// replace null with ""
			if (variables.get(key) == null) {
				processed.replace(key, "");
			} else if (variables.get(key).isEmpty()) {
				processed.replace(key, "");
			}
			if (varsWhichNeedRepeatedProcessing.containsKey(key)) {
				for (String repeatedValue : varsWhichNeedRepeatedProcessing.get(key)) {
					processed.put(repeatedValue, processed.get(key));
				}
			}
		}
		// Process the modifiers
		Map<String, String> modifiedValues = new HashMap<>(processed);
		for (String key : processed.keySet()) {
			PROCESSORS.forEach((suffix, processor) -> {
				modifiedValues.put(key + "?" + suffix, processor.apply(processed.get(key)));
			});
		}

		try {
			return prefix + engine.instantiate(modifiedValues).replace(SLASH_CODE, "/");
		} catch (ValidationException e) {
			throw new IllegalArgumentException("Invalid bindings", e);
		}
	}

	Map<String, ValueProcessor> PROCESSORS = Map.of( //
			"uc", new ValueProcessor(0, String::toUpperCase), //
			"lc", new ValueProcessor(0, String::toLowerCase), //
			"ucfirst", new ValueProcessor(0, s -> s.substring(0, 1).toUpperCase() + s.substring(1)), //
			"lcfirst", new ValueProcessor(0, s -> s.substring(0, 1).toLowerCase() + s.substring(1)), //
			"slashok", new ValueProcessor(0, s -> s.replace("/", SLASH_CODE)) //
	);

	private static boolean occurrencesOfCharGreaterThan(String s, char c, int n) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				count++;
			}
			if (count > n) {
				return true;
			}
		}
		return false;
	}

}

final class ValueProcessor {

	private final int minimumLength;
	private final Function<String, String> processor;

	ValueProcessor(int minimumLength, Function<String, String> processor) {
		this.minimumLength = minimumLength;
		this.processor = processor;
	}

	public String apply(String s) {
		if (s == null || s.isEmpty() || s.length() < minimumLength) {
			return s;
		}
		return processor.apply(s);
	}
}
