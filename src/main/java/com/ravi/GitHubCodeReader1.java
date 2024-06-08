package com.ravi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GitHubCodeReader1 {
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String REPO_OWNER = "poc74";
    private static final String REPO_NAME = "springboot";
    private static final String BRANCH = "wxw"; // or any branch you want to read from
    private static final String GITHUB_TOKEN = "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"; // Add your GitHub token here

    public static void main(String[] args) {
        GitHubCodeReader1 reader = new GitHubCodeReader1();
        String code = reader.extracted("src");
        System.out.println(code);
    }

    private String extracted(String directoryPath) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            List<String> fileUrls = getFileUrls(directoryPath);
            contentBuilder.append(fileUrls.stream()
                    .map(this::getFileContent)
                    .map(content -> {
                        System.out.println(content);
                        String minifiedContent = minifyCode(content);
                        String compressedContent = compressAndEncode(minifiedContent);
                        System.out.println("Minified Content: \n" + minifiedContent);
                        System.out.println("----------------------------------");
                        return content;
                    })
                    .collect(Collectors.joining("\n\n")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private List<String> getFileUrls(String directoryPath) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        fetchFileUrls(directoryPath, fileUrls);
        return fileUrls;
    }

    private void fetchFileUrls(String directoryPath, List<String> fileUrls) throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/contents/%s?ref=%s", GITHUB_API_URL, REPO_OWNER, REPO_NAME, directoryPath, BRANCH);
        System.out.println("Requesting URL: " + apiUrl); // Debug output
        String jsonResponse = makeHttpRequest(apiUrl);
        System.out.println("Response: " + jsonResponse); // Debug output

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        for (JsonNode node : rootNode) {
            String type = node.get("type").asText();
            if (type.equals("file")) {
                fileUrls.add(node.get("download_url").asText());
            } else if (type.equals("dir")) {
                fetchFileUrls(node.get("path").asText(), fileUrls);
            }
        }
    }

    private String getFileContent(String fileUrl) {
        try {
            return makeHttpRequest(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String makeHttpRequest(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "token " + GITHUB_TOKEN);
        HttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 401) {
            System.err.println("Unauthorized: Check your token and permissions.");
        } else if (statusCode == 404) {
            System.err.println("Not Found: Check your URL, repository owner, repository name, and branch.");
        } else if (statusCode != 200) {
            System.err.println("Error: Received HTTP status code " + statusCode);
        }

        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }

    private String minifyCode(String content) {
        // Implement your minification logic here
        return content.replaceAll("\\s+", " ");
    }

    private String compressAndEncode(String content) {
        // Implement your compression and encoding logic here
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}