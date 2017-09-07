package org.silkframework.dataset.rdf;

import org.silkframework.runtime.plugin.EnumerationParameterType;

public enum EntityRetrieverStrategy implements EnumerationParameterType {

    simple("simple", "simple"),

    subQuery("subQuery", "sub-query"),

    parallel("parallel", "parallel");

    private String id;

    private String displayName;

    EntityRetrieverStrategy(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }

    public String displayName() { return displayName; }
}
