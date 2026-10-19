package com.jichi.prompt.service;

import com.jichi.prompt.entity.ContactInfo;
import com.jichi.prompt.entity.ContractInfo;
import com.jichi.prompt.entity.ValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ExtractionValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+");

    public ValidationResult validate(ContactInfo info) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (info.email() != null && !EMAIL_PATTERN.matcher(info.email()).matches()) {
            errors.add("邮箱格式不合法：" + info.email());
        }

        if (info.phone() != null && info.phone().length() != 11) {
            errors.add("电话号码应为11位，实际：" + info.phone().length() + "位");
        }

        if (info.name() != null && (info.name().length() < 2 || info.name().length() > 20)) {
            warnings.add("姓名长度异常：" + info.name());
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    public ValidationResult validate(ContractInfo info) {
        List<String> errors = new ArrayList<>();

        if (info.signDate() != null && info.effectiveDate() != null) {
            if (info.signDate().compareTo(info.effectiveDate()) > 0) {
                errors.add("签署日期晚于生效日期，请核查");
            }
        }

        if (info.contractNumber() == null || info.contractNumber().isBlank()) {
            errors.add("未能提取到合同编号");
        }

        return new ValidationResult(errors.isEmpty(), errors, List.of());
    }
}