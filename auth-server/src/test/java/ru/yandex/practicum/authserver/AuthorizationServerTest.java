package ru.yandex.practicum.authserver;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationServerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void clientCredentialsFlowIssuesJwtForStorefrontClient() {
        ResponseEntity<Map> response = requestToken("storefront", "storefront-secret",
                "payment.read payment.write");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("access_token");

        String token = (String) response.getBody().get("access_token");
        assertThat(token).isNotBlank();

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getClaimAsString("sub")).isEqualTo("storefront");
        assertThat(jwt.getClaimAsStringList("scope")).contains("payment.read", "payment.write");
    }

    @Test
    void tokenRequestWithWrongSecretIsRejected() {
        ResponseEntity<Map> response = requestToken("storefront", "wrong-secret", "payment.read");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> requestToken(String clientId, String clientSecret, String scope) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", scope);

        return restTemplate.postForEntity("http://localhost:" + port + "/oauth2/token",
                new HttpEntity<>(body, headers), Map.class);
    }
}
