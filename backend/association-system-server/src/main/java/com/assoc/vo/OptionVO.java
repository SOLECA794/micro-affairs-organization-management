package com.assoc.vo;

/** 下拉选项出参（公开接口，仅暴露 id 与名称，不含任何 PII） */
public record OptionVO(
        Long id,
        String name) {
}
