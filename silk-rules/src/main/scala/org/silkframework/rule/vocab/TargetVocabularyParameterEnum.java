package org.silkframework.rule.vocab;

import org.silkframework.runtime.plugin.EnumerationParameterType;

/**
 * Enum that specifies the vocabulary category a transformation has enabled.
 */
public enum TargetVocabularyParameterEnum implements EnumerationParameterType {
  disabled("disabled", "disabled"),
  allActive("allActive", "All active vocabularies");

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
