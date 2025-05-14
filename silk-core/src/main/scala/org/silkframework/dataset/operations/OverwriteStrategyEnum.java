package org.silkframework.dataset.operations;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;

public enum OverwriteStrategyEnum implements EnumerationParameterType {

    overwrite("overwrite", "Overwrite existing files"),
    overwriteWithWarning("overwriteWithWarning", "Overwrite existing files with warning"),
    ignoreExisting("ignoreExisting", "Ignore existing files"),
    fail("fail", "Fail if a file already exists");

    private final String id;
    private final String displayName;

    OverwriteStrategyEnum(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }

    public String displayName() { return displayName; }
}