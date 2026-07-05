package com.mmt.core.data.model.translated;

import com.mmt.core.data.model.TranslateMethod;

/**
 * translated.json 键级数据模型
 */
public class TranslatedEntry {
    private String value;
    private String method;
    private String sourceValue;

    public TranslatedEntry() {
    }

    public TranslatedEntry(String value, String method, String sourceValue) {
        this.value = value;
        this.method = method;
        this.sourceValue = sourceValue;
    }

    public TranslatedEntry(String value, TranslateMethod method, String sourceValue) {
        this.value = value;
        this.method = method != null ? method.name() : null;
        this.sourceValue = sourceValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setMethod(TranslateMethod method) {
        this.method = method != null ? method.name() : null;
    }

    public TranslateMethod getMethodEnum() {
        return TranslateMethod.fromString(method);
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }
}