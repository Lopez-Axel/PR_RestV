package com.example.demo_pam.controller;

import com.example.demo_pam.dto.response.StockAlertOverviewDTO;
import com.example.demo_pam.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StockAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockAlertService stockAlertService;

    @Test
    void getAlerts_asAdmin_returnsOverview() throws Exception {
        when(stockAlertService.getOverview()).thenReturn(new StockAlertOverviewDTO(List.of(), List.of()));

        mockMvc.perform(get("/ingredients/alerts").with(user("admin@test.com").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.notifications").isArray());
    }

    @Test
    void getAlerts_asChef_returnsOverview() throws Exception {
        when(stockAlertService.getOverview()).thenReturn(new StockAlertOverviewDTO(List.of(), List.of()));

        mockMvc.perform(get("/ingredients/alerts").with(user("chef@test.com").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("CHEF"))))
                .andExpect(status().isOk());
    }

    @Test
    void getAlerts_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/ingredients/alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAlerts_asWaiter_returnsForbidden() throws Exception {
        mockMvc.perform(get("/ingredients/alerts").with(user("waiter@test.com").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("WAITER"))))
                .andExpect(status().isForbidden());
    }
}
