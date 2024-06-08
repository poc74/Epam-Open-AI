package com.ravi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Component
public class OpenAIClient {

    @Value("${project.path}")
    String directoryPath;

    public  String callIndo() {
        return extracted(directoryPath);
    }

    private  String extracted(String directoryPath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            contentBuilder.append(paths.filter(Files::isRegularFile)
                    .filter(path -> path.getParent().toString().contains("src"))
                    .map(path -> {
                        try {
                            String content = new String(Files.readAllBytes(path));
                            String minifiedContent = minifyCode(content);
                            String compressedContent = compressAndEncode(minifiedContent);
                            System.out.println("File: " + path.getFileName());
                            System.out.println("Minified Content: \n" + minifiedContent);
                            System.out.println("----------------------------------");
                            return content;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return "";
                        }
                    })
                    .collect(Collectors.joining("\n\n")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static String minifyCode(String code) {
        // Remove comments
        String noComments = code.replaceAll("(?s)/\\*.*?\\*/|//.*", "");
        // Remove extra whitespace and newlines
        String minified = noComments.replaceAll("\\s+", " ").replaceAll("\\s*([{};(),])\\s*", "$1").trim();
        return minified;
    }

    public static String compressAndEncode(String content) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(content.getBytes());
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\Ravinder_Sabbani\\Desktop\\Azure-OpenAI-Java-Spring-Sample-for-Chat-GPT-4-main";


        String extractedContent = new OpenAIClient().extracted(directoryPath);
        System.out.println("Extracted Content: \n" + extractedContent);

        int a = 10/0;
        System.out.println(a);
    }
}



