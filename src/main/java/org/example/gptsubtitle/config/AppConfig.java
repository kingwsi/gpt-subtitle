package org.example.gptsubtitle.config;

import lombok.Data;

import java.util.Map;

@Data
public class AppConfig {
    private String prompt = "你是一位翻译助手，结合上下文翻译内容到中文，尽量符合中文使用习惯。";
    private String baseApi = "http://localhost:1234/v1";
    private String appKey = "1234";
    private int chunkSize = 10;
    private int messageMaxSize = 50;
    private int maxTokens = 4096;
    private String model = "qwen2.5-7b-instruct-1m";
    /**
     * 开启jsonSchema，更精确控制api返回数据格式
     * <a href="https://github.com/ggml-org/llama.cpp/blob/master/grammars/README.md">llama.cpp支持</a>
     */
    private boolean enableJsonSchema;
    /**
     * 自定义词典
     */
    private Map<String, String> dictionary;
} 