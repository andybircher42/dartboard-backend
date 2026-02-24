package com.bookishbroccoli.freshness;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class FreshnessChecker {

	private final Clock clock;

	public FreshnessChecker() {
		this(Clock.systemUTC());
	}

	FreshnessChecker(Clock clock) {
		this.clock = clock;
	}

	public FreshnessResult check(Instant timestamp, FreshnessPolicy policy) {
		if (timestamp == null) {
			return policy.isStaleWhenMissing()
					? FreshnessResult.stale("no timestamp available")
					: FreshnessResult.fresh(null, null);
		}

		Instant now = clock.instant();
		if (timestamp.isAfter(now)) {
			return FreshnessResult.fresh(timestamp, Duration.ZERO);
		}

		Duration age = Duration.between(timestamp, now);
		if (age.compareTo(policy.getMaxAge()) <= 0) {
			return FreshnessResult.fresh(timestamp, age);
		}

		return FreshnessResult.stale(timestamp, age,
				"age " + age + " exceeds max " + policy.getMaxAge());
	}

	public FreshnessResult check(String isoTimestamp, FreshnessPolicy policy) {
		if (isoTimestamp == null || isoTimestamp.isBlank()) {
			return policy.isStaleWhenMissing()
					? FreshnessResult.stale("no timestamp available")
					: FreshnessResult.fresh(null, null);
		}

		Optional<Instant> parsed = parseIsoOrLocalDate(isoTimestamp);
		if (parsed.isEmpty()) {
			return FreshnessResult.stale("unparseable timestamp: " + isoTimestamp);
		}

		return check(parsed.get(), policy);
	}

	public FreshnessResult check(Object value, FreshnessPolicy policy) {
		if (value == null) {
			return check((Instant) null, policy);
		}
		if (value instanceof Instant instant) {
			return check(instant, policy);
		}
		if (value instanceof String s) {
			return check(s, policy);
		}
		if (value instanceof Long epochMillis) {
			return check(Instant.ofEpochMilli(epochMillis), policy);
		}
		if (value instanceof Date date) {
			return check(date.toInstant(), policy);
		}
		return FreshnessResult.stale("unsupported timestamp type: " + value.getClass().getSimpleName());
	}

	public <T> List<T> findStale(List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
		return items.stream()
				.filter(item -> check(extractor.apply(item), policy).stale())
				.toList();
	}

	public <T> List<T> findFresh(List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
		return items.stream()
				.filter(item -> check(extractor.apply(item), policy).isFresh())
				.toList();
	}

	public <T> Optional<FreshnessResult> checkMostRecent(List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
		if (items.isEmpty()) {
			return Optional.empty();
		}

		Optional<Instant> mostRecent = items.stream()
				.map(extractor)
				.filter(ts -> ts != null)
				.max(Instant::compareTo);

		if (mostRecent.isEmpty()) {
			return Optional.of(check((Instant) null, policy));
		}

		return Optional.of(check(mostRecent.get(), policy));
	}

	public static Optional<Instant> parseTimestamp(Object value) {
		if (value == null) {
			return Optional.empty();
		}
		if (value instanceof Instant instant) {
			return Optional.of(instant);
		}
		if (value instanceof Long epochMillis) {
			return Optional.of(Instant.ofEpochMilli(epochMillis));
		}
		if (value instanceof Date date) {
			return Optional.of(date.toInstant());
		}
		if (value instanceof String s) {
			return parseIsoOrLocalDate(s);
		}
		return Optional.empty();
	}

	private static Optional<Instant> parseIsoOrLocalDate(String value) {
		try {
			return Optional.of(Instant.parse(value));
		} catch (DateTimeParseException e) {
			// fall through to LocalDate attempt
		}
		try {
			return Optional.of(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}
}
