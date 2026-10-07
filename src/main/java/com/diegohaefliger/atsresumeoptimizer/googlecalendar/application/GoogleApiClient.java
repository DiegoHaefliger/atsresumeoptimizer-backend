package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleApiException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthRevokedException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthorizationException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCalendarItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleApiClient implements GoogleCalendarGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleApiClient.class);
	private static final int MAX_LISTED_EVENTS = 250;
	private static final String SCOPES = "openid email https://www.googleapis.com/auth/calendar.events";
	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private final GoogleCalendarProperties properties;
	private final RestClient http;

	@Autowired
	GoogleApiClient(GoogleCalendarProperties properties) {
		this(properties, RestClient.builder());
	}

	GoogleApiClient(GoogleCalendarProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		this.http = builder.build();
	}

	@Override
	public String authorizationUrl(GoogleCredentials credentials, String state) {
		return UriComponentsBuilder.fromUriString(properties.authUrl())
				.queryParam("client_id", credentials.clientId())
				.queryParam("redirect_uri", properties.redirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", SCOPES)
				.queryParam("access_type", "offline")
				.queryParam("prompt", "consent")
				.queryParam("state", state)
				.encode()
				.build()
				.toUriString();
	}

	@Override
	public GoogleTokens exchangeCode(GoogleCredentials credentials, String code) {
		MultiValueMap<String, String> form = clientForm(credentials);
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", properties.redirectUri());
		Map<String, Object> response;
		try {
			response = postForm(form);
		} catch (RestClientResponseException exception) {
			throw new GoogleAuthorizationException("O Google recusou o código de autorização.");
		}
		String accessToken = text(response, "access_token");
		String refreshToken = text(response, "refresh_token");
		if (accessToken == null || refreshToken == null) {
			throw new GoogleAuthorizationException("O Google não devolveu o acesso offline. Tente conectar de novo.");
		}
		return new GoogleTokens(accessToken, refreshToken);
	}

	@Override
	public String refreshAccessToken(GoogleCredentials credentials, String refreshToken) {
		MultiValueMap<String, String> form = clientForm(credentials);
		form.add("grant_type", "refresh_token");
		form.add("refresh_token", refreshToken);
		try {
			return text(postForm(form), "access_token");
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 400 || exception.getStatusCode().value() == 401) {
				throw new GoogleAuthRevokedException();
			}
			throw new GoogleApiException("Falha ao renovar o acesso ao Google.", exception);
		}
	}

	@Override
	public String accountEmail(String accessToken) {
		try {
			return text(http.get()
					.uri(properties.userinfoUrl())
					.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
					.retrieve()
					.body(JSON_OBJECT), "email");
		} catch (RestClientException exception) {
			throw new GoogleApiException("Falha ao ler a conta do Google.", exception);
		}
	}

	@Override
	public String saveEvent(String accessToken, String eventId, Map<String, Object> body) {
		try {
			if (eventId != null) {
				try {
					return text(send(http.put().uri(properties.eventsUrl() + "/{id}", eventId), accessToken, body), "id");
				} catch (RestClientResponseException exception) {
					if (!isGone(exception.getStatusCode())) {
						throw exception;
					}
				}
			}
			return text(send(http.post().uri(properties.eventsUrl()), accessToken, body), "id");
		} catch (RestClientException exception) {
			throw new GoogleApiException("Falha ao gravar o evento no Google Agenda.", exception);
		}
	}

	@Override
	public void deleteEvent(String accessToken, String eventId) {
		try {
			http.delete()
					.uri(properties.eventsUrl() + "/{id}", eventId)
					.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			if (!isGone(exception.getStatusCode())) {
				throw new GoogleApiException("Falha ao remover o evento do Google Agenda.", exception);
			}
		} catch (RestClientException exception) {
			throw new GoogleApiException("Falha ao remover o evento do Google Agenda.", exception);
		}
	}

	@Override
	public List<GoogleCalendarItem> listEvents(String accessToken, Instant from, Instant to) {
		try {
			Map<String, Object> body = http.get()
					.uri(UriComponentsBuilder.fromUriString(properties.eventsUrl())
							.queryParam("timeMin", from.toString())
							.queryParam("timeMax", to.toString())
							.queryParam("singleEvents", true)
							.queryParam("orderBy", "startTime")
							.queryParam("maxResults", MAX_LISTED_EVENTS)
							.encode()
							.build()
							.toUri())
					.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
					.retrieve()
					.body(JSON_OBJECT);
			Object items = body == null ? null : body.get("items");
			if (!(items instanceof List<?> list)) {
				return List.of();
			}
			return list.stream()
					.filter(Map.class::isInstance)
					.map(item -> (Map<?, ?>) item)
					.filter(item -> !"cancelled".equals(item.get("status")))
					.map(GoogleApiClient::toItem)
					.filter(Objects::nonNull)
					.toList();
		} catch (RestClientException exception) {
			throw new GoogleApiException("Falha ao ler a agenda do Google.", exception);
		}
	}

	private static GoogleCalendarItem toItem(Map<?, ?> event) {
		if (!(event.get("start") instanceof Map<?, ?> start) || !(event.get("end") instanceof Map<?, ?> end)) {
			return null;
		}
		boolean allDay = start.get("dateTime") == null;
		Object startValue = allDay ? start.get("date") : start.get("dateTime");
		Object endValue = allDay ? end.get("date") : end.get("dateTime");
		if (startValue == null || endValue == null) {
			return null;
		}
		Object title = event.get("summary");
		Object link = event.get("htmlLink");
		return new GoogleCalendarItem(String.valueOf(event.get("id")), title == null ? "(sem título)" : title.toString(),
				startValue.toString(), endValue.toString(), allDay, link == null ? null : link.toString());
	}

	@Override
	public void revoke(String token) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("token", token);
		try {
			http.post()
					.uri(properties.revokeUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException exception) {
			LOGGER.warn("Não foi possível revogar o token no Google: {}", exception.getMessage());
		}
	}

	private Map<String, Object> postForm(MultiValueMap<String, String> form) {
		return http.post()
				.uri(properties.tokenUrl())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(JSON_OBJECT);
	}

	private static Map<String, Object> send(RestClient.RequestBodySpec spec, String accessToken, Map<String, Object> body) {
		return spec.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(JSON_OBJECT);
	}

	private static MultiValueMap<String, String> clientForm(GoogleCredentials credentials) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", credentials.clientId());
		form.add("client_secret", credentials.clientSecret());
		return form;
	}

	private static boolean isGone(HttpStatusCode status) {
		return status.value() == 404 || status.value() == 410;
	}

	private static String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private static String text(Map<String, Object> body, String field) {
		Object value = body == null ? null : body.get(field);
		return value == null ? null : value.toString();
	}
}
