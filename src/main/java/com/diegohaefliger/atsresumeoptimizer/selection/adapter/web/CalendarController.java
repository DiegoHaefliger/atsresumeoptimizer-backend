package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.CalendarFeedUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
class CalendarController {

	private static final String FEED_PATH = "/api/v1/calendar/feed.ics?token=";
	private static final Duration FEED_PAST = Duration.ofDays(90);
	private static final Duration FEED_FUTURE = Duration.ofDays(270);

	private final SelectionCalendar calendar;
	private final CalendarWebMapper mapper;
	private final IcsFeedWriter feedWriter;
	private final String feedToken;

	CalendarController(SelectionCalendar calendar, CalendarWebMapper mapper, IcsFeedWriter feedWriter,
			@Value("${app.calendar.feed-token:}") String feedToken) {
		this.calendar = calendar;
		this.mapper = mapper;
		this.feedWriter = feedWriter;
		this.feedToken = feedToken;
	}

	@GetMapping("/events")
	List<CalendarEventResponse> events(@RequestParam Instant from, @RequestParam Instant to) {
		return calendar.between(from, to).stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/feed-info")
	CalendarFeedInfoResponse feedInfo() {
		return feedToken.isBlank()
				? new CalendarFeedInfoResponse(false, null)
				: new CalendarFeedInfoResponse(true, FEED_PATH + feedToken);
	}

	@GetMapping(path = "/feed.ics", produces = "text/calendar;charset=UTF-8")
	String feed(@RequestParam String token) {
		if (feedToken.isBlank() || !tokenMatches(token)) {
			throw new CalendarFeedUnavailableException();
		}
		Instant now = Instant.now();
		return feedWriter.write(calendar.between(now.minus(FEED_PAST), now.plus(FEED_FUTURE)), now);
	}

	private boolean tokenMatches(String token) {
		return MessageDigest.isEqual(feedToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
	}
}
