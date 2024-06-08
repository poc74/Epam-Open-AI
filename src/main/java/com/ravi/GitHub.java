package com.ravi;

import com.ravi.request.CreateRepoRequest1;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GitHub {

    private static final String GITHUB_API_URL = "https://api.github.com";

    private static final String USERNAME = "poc74";

    private static final String ACCESS_TOKEN = "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";


    @PostMapping("/createRepo")
    public ResponseEntity<String> createRepo(@RequestBody CreateRepoRequest1 request) {
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        String url = GITHUB_API_URL + "/user/repos";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + ACCESS_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", request.getRepoName());
        requestBody.put("private", true);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            return ResponseEntity.ok("Repository '" + request.getRepoName() + "' created successfully!");
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }
}
