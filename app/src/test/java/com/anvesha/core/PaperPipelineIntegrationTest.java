package com.anvesha.core;

import com.anvesha.core.dto.auth.AuthResponse;
import com.anvesha.core.dto.auth.LoginRequest;
import com.anvesha.core.dto.auth.RegisterRequest;
import com.anvesha.core.entity.Institution;
import com.anvesha.core.repository.InstitutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test for the paper → molecule → novelty scan pipeline.
 * The Python chemistry service is mocked with WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PaperPipelineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer wireMock;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InstitutionRepository institutionRepository;

    private String jwt;
    private Long institutionId;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overridePythonServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("anvesha.python-service.url", wireMock::baseUrl);
    }

    @BeforeEach
    void setUp() throws Exception {
        Institution inst = institutionRepository.save(
                Institution.builder().name("CSIR-NCL").type("CSIR_LAB").build());
        institutionId = inst.getId();

        RegisterRequest reg = new RegisterRequest(
                "Dr. Pipeline", "pipeline@csir.ac.in",
                "pass123", institutionId, "RESEARCHER");

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn().getResponse().getContentAsString();

        jwt = objectMapper.readValue(body, AuthResponse.class).accessToken();

        // Stub /extract-molecules
        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/extract-molecules"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "molecules": [
                                {
                                  "extracted_name_raw": "2-acetoxybenzoic acid",
                                  "iupac_name": "2-(acetyloxy)benzoic acid",
                                  "smiles": "CC(=O)Oc1ccccc1C(=O)O",
                                  "extraction_confidence": 0.92
                                }
                              ]
                            }
                            """)));

        // Stub /novelty-check — aspirin is well-known, so NOT novel
        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/novelty-check"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "novelty_score": 0.02,
                              "is_novel": false,
                              "closest_match_source": "PUBCHEM",
                              "closest_match_id": "2244",
                              "tanimoto_similarity": 0.98
                            }
                            """)));
    }

    @Test
    void upload_paper_creates_molecule_and_scan() throws Exception {
        // Upload paper
        var uploadResult = mockMvc.perform(multipart("/api/papers")
                        .param("title", "Synthesis of Novel Aspirin Derivatives")
                        .param("sourceType", "JOURNAL")
                        .param("rawText", "We synthesized 2-acetoxybenzoic acid and tested its properties.")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long paperId = objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()).get("id").asLong();

        // Give async processing a moment to complete
        Thread.sleep(3000);

        // Verify paper + molecules via GET
        mockMvc.perform(get("/api/papers/" + paperId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.molecules").isArray())
                .andExpect(jsonPath("$.molecules[0].extractedNameRaw").value("2-acetoxybenzoic acid"))
                .andExpect(jsonPath("$.molecules[0].smiles").value("CC(=O)Oc1ccccc1C(=O)O"))
                .andExpect(jsonPath("$.molecules[0].latestScan.isNovel").value(false))
                .andExpect(jsonPath("$.molecules[0].latestScan.tanimotoSimilarity").value(0.98));
    }
}
