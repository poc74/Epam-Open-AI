package com.ravi.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter @Getter
public class OpenAIMessages implements Serializable {
    private List<Message> messages;  

    private double temperature;  
    private double top_p;  
    private double presence_penalty;  
    private double frequency_penalty;  
    private int max_tokens;  
    private String stop;
    private boolean stream;
}
