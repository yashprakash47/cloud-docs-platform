package com.clouddocs.tenancy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ALICE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID CAROL = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID DAVE = UUID.fromString("10000000-0000-0000-0000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedTwoTenants() {
        jdbcTemplate.update("DELETE FROM document_tags");
        jdbcTemplate.update("UPDATE documents SET current_version_id=NULL");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM folders");
        jdbcTemplate.update("DELETE FROM tags");
        jdbcTemplate.update("DELETE FROM audit_events");
        jdbcTemplate.update("DELETE FROM memberships");
        jdbcTemplate.update("DELETE FROM tenants");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(ALICE, "alice@example.com");
        insertUser(BOB, "bob@example.com");
        insertUser(CAROL, "carol@example.com");
        insertUser(DAVE, "dave@example.com");
        insertTenant(TENANT_A, "Tenant A");
        insertTenant(TENANT_B, "Tenant B");
        insertMembership("20000000-0000-0000-0000-000000000001", ALICE, TENANT_A, "TENANT_ADMIN");
        insertMembership("20000000-0000-0000-0000-000000000002", BOB, TENANT_A, "MEMBER");
        insertMembership("20000000-0000-0000-0000-000000000003", CAROL, TENANT_A, "VIEWER");
        insertMembership("20000000-0000-0000-0000-000000000004", BOB, TENANT_B, "TENANT_ADMIN");
    }

    @Test
    void currentUserAndAccessibleTenantsAreTenantScoped() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-User-Email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        mockMvc.perform(get("/api/v1/tenants").header("X-User-Email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tenant A"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void userCannotAccessAnotherTenantsMemberships() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{tenantId}/memberships", TENANT_A)
                        .header("X-User-Email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_A.toString()));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/memberships", TENANT_B)
                        .header("X-User-Email", "alice@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberAndViewerCannotManageMembershipsButAdminCan() throws Exception {
        String body = "{\"email\":\"dave@example.com\",\"role\":\"MEMBER\"}";

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/memberships", TENANT_A)
                        .header("X-User-Email", "bob@example.com")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/memberships", TENANT_A)
                        .header("X-User-Email", "carol@example.com")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/memberships", TENANT_A)
                        .header("X-User-Email", "alice@example.com")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void missingLocalIdentityIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    private void insertUser(UUID id, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, display_name, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, email, email.substring(0, email.indexOf('@')));
    }

    private void insertTenant(UUID id, String name) {
        jdbcTemplate.update("INSERT INTO tenants (id, name) VALUES (?, ?)", id, name);
    }

    private void insertMembership(String id, UUID userId, UUID tenantId, String role) {
        jdbcTemplate.update(
                "INSERT INTO memberships (id, user_id, tenant_id, role) VALUES (?, ?, ?, ?)",
                UUID.fromString(id), userId, tenantId, role);
    }
}
