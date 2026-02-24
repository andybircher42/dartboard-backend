package com.bookishbroccoli.service;

import com.bookishbroccoli.retry.RetryExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Field;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

abstract class ApifyServiceTestBase {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	protected ApifyService service;
	protected final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception {
		service = new ApifyService(objectMapper, new RetryExecutor(ms -> {}));
		setField(service, "apifyToken", "test-token");
		setField(service, "apifyApiBase", wireMock.baseUrl());
		setField(service, "pollIntervalSeconds", 1);
		setField(service, "pollTimeoutSeconds", 5);
	}

	protected static void setField(Object target, String fieldName, Object value) throws Exception {
		Class<?> clazz = target.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
