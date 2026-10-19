package com.jichi.langchain4j.service;

import com.jichi.langchain4j.model.NamedEntity;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface EntityExtractor {

    @SystemMessage("从文本中提取所有命名实体（人名、地名、公司名、产品名）")
    List<NamedEntity> extract(String text);
}