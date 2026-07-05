package com.mmt.core.data.model.packed;

import com.mmt.core.data.model.TranslateMethod;

/**
 * packed.json 键级数据模型
 */
public class PackedEntry {
    private String value;
    private String method;
    private String valueHash;
    private String sourceValue;

    public PackedEntry() {
    }

    public PackedEntry(String value, String method, String valueHash, String sourceValue) {
        this.value = value;
        this.method = method;
        this.valueHash = valueHash;
        this.sourceValue = sourceValue;
    }

    public PackedEntry(String value, TranslateMethod method, String valueHash, String sourceValue) {
        this.value = value;
        this.method = method != null ? method.name() : null;
        this.valueHash = valueHash;
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

    public String getValueHash() {
        return valueHash;
    }

    public void setValueHash(String valueHash) {
        this.valueHash = valueHash;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }
}