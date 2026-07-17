package org.silkframework.runtime.plugin.types;

/**
 * Parameter Enum to represent comparison orders.
 */
public enum CompareOrder implements EnumerationParameterType {

  alphabetical("Alphabetical", "Alphabetical"),
  numerical("Numerical", "Numerical"),
  integer("Integer", "Integer"),
  autodetect("Autodetect", "Autodetect");

  private final String id;
  private final String displayName;

  CompareOrder(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  /**
   * True if v1 is lower than v2 (or equal, if orEqual is set) according to this order.
   * In numerical/integer order, values that cannot be parsed are never lower.
   */
  public boolean isLower(String v1, String v2, boolean orEqual) {
    return switch (this) {
      case alphabetical -> isLowerAlphabetical(v1, v2, orEqual);
      case numerical -> isLowerNumerical(v1, v2, orEqual);
      case integer -> isLowerInteger(v1, v2, orEqual);
      case autodetect -> isLowerAutodetect(v1, v2, orEqual);
    };
  }

  private static boolean isLowerAlphabetical(String v1, String v2, boolean orEqual) {
    int comparison = v1.compareTo(v2);
    return orEqual ? comparison <= 0 : comparison < 0;
  }

  private static boolean isLowerNumerical(String v1, String v2, boolean orEqual) {
    Double n1 = parseDouble(v1);
    Double n2 = parseDouble(v2);
    if (n1 == null || n2 == null) {
      return false;
    }
    return orEqual ? n1 <= n2 : n1 < n2;
  }

  private static boolean isLowerInteger(String v1, String v2, boolean orEqual) {
    Integer n1 = parseInt(v1);
    Integer n2 = parseInt(v2);
    if (n1 == null || n2 == null) {
      return false;
    }
    return orEqual ? n1 <= n2 : n1 < n2;
  }

  private static boolean isLowerAutodetect(String v1, String v2, boolean orEqual) {
    Double n1 = parseDouble(v1);
    Double n2 = parseDouble(v2);
    if (n1 != null && n2 != null) {
      return orEqual ? n1 <= n2 : n1 < n2;
    }
    return isLowerAlphabetical(v1, v2, orEqual);
  }

  private static Double parseDouble(String str) {
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Integer parseInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }
}
