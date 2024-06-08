package com.ravi;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public class AngularDATA {

    @GetMapping("/hello")
    public String getString(){
        return "hello";
    }

    @GetMapping("/diagram")
    public String generateMermaidDiagram() {
        // Generate the mermaid diagram code
        String mermaidDiagram = "graph TD\n" +
                "A[Angular] -->|Calls| B[Spring Boot]\n" +
                "B -->|Responds| A";
        return mermaidDiagram;
    }
}
