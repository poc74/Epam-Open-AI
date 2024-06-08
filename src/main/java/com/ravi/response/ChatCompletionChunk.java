package com.ravi.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter @Getter
public class ChatCompletionChunk {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Object usage;

}