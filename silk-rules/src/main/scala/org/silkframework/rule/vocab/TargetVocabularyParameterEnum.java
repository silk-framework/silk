package org.silkframework.rule.vocab;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;

/**
 * Enum that specifies the vocabulary category a transformation has enabled.
 */
public enum TargetVocabularyParameterEnum implements EnumerationParameterType {
  noVocabularies("no vocabularies", "No vocabularies"),
  allInstalled("all installed vocabularies", "All installed vocabularies");

  private final String id;
  private final String displayName;

  TargetVocabularyParameterEnum(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }
}
