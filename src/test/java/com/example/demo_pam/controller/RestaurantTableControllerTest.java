package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.RestaurantTableRequest;
import com.example.demo_pam.dto.response.RestaurantTableResponse;
import com.example.demo_pam.service.RestaurantTableService;
import com.example.demo_pam.service.TableStatusSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantTableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RestaurantTableService restaurantTableService;

    @MockitoBean
    private TableStatusSseService sseService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createTable_returnsCreatedForAdmin() throws Exception {
        RestaurantTableRequest request = new RestaurantTableRequest(1, 4);
        RestaurantTableResponse response = new RestaurantTableResponse(UUID.randomUUID(), 1, 4, "AVAILABLE");
        when(restaurantTableService.create(any())).thenReturn(response);

        mockMvc.perform(post("/restaurantTable")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "WAITER")
    void createTable_forbidsWaiter() throws Exception {
        RestaurantTableRequest request = new RestaurantTableRequest(1, 4);

        mockMvc.perform(post("/restaurantTable")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(restaurantTableService, never()).create(any());
    }

    @Test
    @WithMockUser(authorities = "CHEF")
    void findAll_allowsAnyAuthenticatedRole() throws Exception {
        when(restaurantTableService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/restaurantTable"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "WAITER")
    void deleteTable_forbidsWaiter() throws Exception {
        mockMvc.perform(delete("/restaurantTable/" + UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
