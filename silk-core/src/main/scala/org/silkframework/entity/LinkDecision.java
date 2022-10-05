package org.silkframework.entity;

/**
 * The label of a reference link.
 */
public enum LinkDecision {

  POSITIVE("positive"),
  NEGATIVE("negative"),
  UNLABELED("unlabeled");

  private final String id;

  LinkDecision(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static LinkDecision fromId(String id) {
    switch (id) {
      case "positive":
        return POSITIVE;
      case "negative":
        return NEGATIVE;
      case "unlabeled":
        return UNLABELED;
      default:
        throw new IllegalArgumentException("Invalid LinkDecision identifier '" + id + "'.");
    }
  }
}
