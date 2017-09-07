package org.silkframework.dataset.rdf;

import org.silkframework.runtime.plugin.EnumerationParameterType;

public enum EntityRetrieverStrategy implements EnumerationParameterType {

    simple("simple"),

    subQuery("sub-query"),

    parallel("parallel");

    private String displayName;

    EntityRetrieverStrategy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }
}
