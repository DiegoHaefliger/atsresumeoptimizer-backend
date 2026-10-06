package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.application.CalendarFeedService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.CalendarFeedUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	private final CalendarFeedService feedService;

	CalendarController(SelectionCalendar calendar, CalendarWebMapper mapper, IcsFeedWriter feedWriter,
			CalendarFeedService feedService) {
		this.calendar = calendar;
		this.mapper = mapper;
		this.feedWriter = feedWriter;
		this.feedService = feedService;
	}

	@GetMapping("/events")
	List<CalendarEventResponse> events(@RequestParam Instant from, @RequestParam Instant to) {
		return calendar.between(from, to).stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/feed-info")
	CalendarFeedInfoResponse feedInfo() {
		return feedInfoOf(feedService.token());
	}

	@PostMapping("/feed/token")
	CalendarFeedInfoResponse regenerateFeedToken() {
		return feedInfoOf(Optional.of(feedService.regenerate()));
	}

	@DeleteMapping("/feed/token")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void disableFeed() {
		feedService.disable();
	}

	@GetMapping(path = "/feed.ics", produces = "text/calendar;charset=UTF-8")
	String feed(@RequestParam String token) {
		if (!feedService.accepts(token)) {
			throw new CalendarFeedUnavailableException();
		}
		Instant now = Instant.now();
		return feedWriter.write(calendar.between(now.minus(FEED_PAST), now.plus(FEED_FUTURE)), now);
	}

	private static CalendarFeedInfoResponse feedInfoOf(Optional<String> token) {
		return token.map(value -> new CalendarFeedInfoResponse(true, FEED_PATH + value))
				.orElseGet(() -> new CalendarFeedInfoResponse(false, null));
	}
}
