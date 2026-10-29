package org.silkframework.plugins.dataset.json;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;

public enum JsonToFileOutputModeEnum implements EnumerationParameterType {

    file("file", "One file per entity"),
    zip("zip", "ZIP archive"),
    jsonArray("jsonArray", "Merged JSON array");

    private final String id;
    private final String displayName;

    JsonToFileOutputModeEnum(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }

    public String displayName() { return displayName; }
}
