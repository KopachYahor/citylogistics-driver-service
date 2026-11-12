package com.kopach.logistics.driver.controller;

import com.kopach.logistics.driver.config.InternalApiKeyFilter;
import com.kopach.logistics.driver.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
@TestPropertySource(properties = "internal.api-key=test-internal-key")
class DriverControllerApiKeyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverRepository driverRepository;

    @Test
    void withoutKey_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void withWrongKey_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/drivers").header(InternalApiKeyFilter.HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withCorrectKey_shouldReturn200() throws Exception {
        when(driverRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/drivers").header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isOk());
    }
}
