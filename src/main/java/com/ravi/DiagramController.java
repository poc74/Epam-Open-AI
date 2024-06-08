package com.ravi;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiagramController {

    @GetMapping("/diagram")
    public String getDiagram(Model model) {
        String mermaidCode = "graph TD;\n" +
                "    A-->B;\n" +
                "    A-->C;\n" +
                "    B-->D;\n" +
                "    C-->D;";
        model.addAttribute("mermaidCode", mermaidCode);
        return "diagram";
    }
}
