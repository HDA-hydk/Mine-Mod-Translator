package com.mmt.core.translate.dict;

public enum DictVersion {
    MINI("mini"),
    FULL("full");

    private final String name;

    DictVersion(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DictVersion fromString(String name) {
        if (name == null) {
            return MINI;
        }
        switch (name.toLowerCase()) {
            case "full":
                return FULL;
            case "mini":
            default:
                return MINI;
        }
    }
}