package com.clouddocs.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class UploadIntegrationTest {
    private static final UUID TENANT_A=UUID.fromString("00000000-0000-0000-0000-000000000001"), TENANT_B=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MEMBER=UUID.fromString("10000000-0000-0000-0000-000000000002"), OTHER=UUID.fromString("10000000-0000-0000-0000-000000000004");
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @BeforeEach void seed(){jdbc.update("DELETE FROM document_tags");jdbc.update("UPDATE documents SET current_version_id=NULL");jdbc.update("DELETE FROM document_versions");jdbc.update("DELETE FROM documents");jdbc.update("DELETE FROM folders");jdbc.update("DELETE FROM tags");jdbc.update("DELETE FROM audit_events");jdbc.update("DELETE FROM memberships");jdbc.update("DELETE FROM tenants");jdbc.update("DELETE FROM users");user(MEMBER,"member@example.com");user(OTHER,"other@example.com");tenant(TENANT_A,"A");tenant(TENANT_B,"B");membership(MEMBER,TENANT_A,"MEMBER");membership(OTHER,TENANT_B,"TENANT_ADMIN");}
    @Test void uploadsCompletesDownloadsAndRejectsOtherTenant() throws Exception {String document=createDocument();String intent=mvc.perform(post("/api/v1/tenants/{t}/documents/{d}/uploads/intents",TENANT_A,document).header("X-User-Email","member@example.com").contentType(APPLICATION_JSON).content("{\"originalFilename\":\"hello.txt\",\"contentType\":\"text/plain\",\"sizeBytes\":5}" )).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*","$1");mvc.perform(multipart("/api/v1/tenants/{t}/documents/{d}/uploads/intents/{i}/content",TENANT_A,document,intent).file(new MockMultipartFile("file","hello.txt","text/plain","hello".getBytes())).header("X-User-Email","member@example.com")).andExpect(status().isNoContent());String version=mvc.perform(post("/api/v1/tenants/{t}/documents/{d}/uploads/intents/{i}/complete",TENANT_A,document,intent).header("X-User-Email","member@example.com")).andExpect(status().isCreated()).andExpect(jsonPath("$.checksum").value("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")).andReturn().getResponse().getContentAsString().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*","$1");mvc.perform(get("/api/v1/tenants/{t}/documents/{d}/versions/{v}/content",TENANT_A,document,version).header("X-User-Email","member@example.com")).andExpect(status().isOk()).andExpect(content().string("hello"));mvc.perform(get("/api/v1/tenants/{t}/documents/{d}/versions/{v}/content",TENANT_A,document,version).header("X-User-Email","other@example.com")).andExpect(status().isForbidden());}
    @Test void rejectsEmptyAndDisallowedContent() throws Exception {String document=createDocument();String endpoint="/api/v1/tenants/"+TENANT_A+"/documents/"+document+"/uploads/intents";mvc.perform(post(endpoint).header("X-User-Email","member@example.com").contentType(APPLICATION_JSON).content("{\"originalFilename\":\"empty.txt\",\"contentType\":\"text/plain\",\"sizeBytes\":0}" )).andExpect(status().isBadRequest());mvc.perform(post(endpoint).header("X-User-Email","member@example.com").contentType(APPLICATION_JSON).content("{\"originalFilename\":\"bad.exe\",\"contentType\":\"application/x-msdownload\",\"sizeBytes\":1}" )).andExpect(status().isBadRequest());}
    @Test void rejectsFilesLargerThanConfiguredLimit() throws Exception {String document=createDocument();mvc.perform(post("/api/v1/tenants/{t}/documents/{d}/uploads/intents",TENANT_A,document).header("X-User-Email","member@example.com").contentType(APPLICATION_JSON).content("{\"originalFilename\":\"large.txt\",\"contentType\":\"text/plain\",\"sizeBytes\":10485761}" )).andExpect(status().isBadRequest());}
    private String createDocument() throws Exception{return mvc.perform(post("/api/v1/tenants/{t}/documents",TENANT_A).header("X-User-Email","member@example.com").contentType(APPLICATION_JSON).content("{\"name\":\"Upload\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*","$1");}
    private void user(UUID id,String email){jdbc.update("INSERT INTO users(id,email,display_name,status) VALUES(?,?,?,'ACTIVE')",id,email,email);}private void tenant(UUID id,String name){jdbc.update("INSERT INTO tenants(id,name) VALUES(?,?)",id,name);}private void membership(UUID user,UUID tenant,String role){jdbc.update("INSERT INTO memberships(id,user_id,tenant_id,role) VALUES(?,?,?,?)",UUID.randomUUID(),user,tenant,role);}
}
