package org.silkframework.rule.plugins.transformer.metadata;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;


public enum FileAttributeEnum implements EnumerationParameterType {
  name("name", "File name"),
  relativePath("relativePath", "Relative path within project files"),
  absolutePath("absolutePath", "Absolute path on file system"),
  size("size", "Size in bytes"),
  modified("modified", "Modified date")
  ;

  final private String id;
  final private String label;

  FileAttributeEnum(String id, String label) {
    this.id = id;
    this.label = label;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public String displayName() {
    return label;
  }
}
