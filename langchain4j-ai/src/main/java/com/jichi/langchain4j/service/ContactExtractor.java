package com.jichi.langchain4j.service;

import com.jichi.langchain4j.model.ContactInfo;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.Optional;

@AiService
public interface ContactExtractor {

    @SystemMessage("从文本中提取联系人信息，如果某个字段找不到，对应字段为 null")
    Optional<ContactInfo> extract(String text);
}