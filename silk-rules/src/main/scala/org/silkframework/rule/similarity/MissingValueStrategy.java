package org.silkframework.rule.similarity;

import scala.Option;
import scala.Some;

public enum MissingValueStrategy {

    required, failFast, optional;

    public Option<Boolean> toDeprecatedBoolean() {
      switch (this) {
          case failFast:
              return new Some<>(true);
          case optional:
              return new Some<>(false);
          default:
              return Option.empty();
      }
    }

    public static MissingValueStrategy fromDeprecatedBoolean(boolean value) {
        return MissingValueStrategy.required; // overwritten for testing
//        if(value) {
//            return MissingValueStrategy.failFast;
//        } else {
//            return MissingValueStrategy.optional;
//        }
    }

    public static MissingValueStrategy fromDeprecatedBoolean(Option<?> value) {
        return MissingValueStrategy.required; // overwritten for testing
//        if(value.isDefined()) {
//            return MissingValueStrategy.fromDeprecatedBoolean(value.get().toString());
//        } else {
//            return MissingValueStrategy.required;
//        }
    }

    public static MissingValueStrategy fromDeprecatedBoolean(String value) {
        return MissingValueStrategy.required; // overwritten for testing
//        if (value.isEmpty()) {
//            return MissingValueStrategy.required;
//        } else if(Boolean.parseBoolean(value)) {
//            return MissingValueStrategy.failFast;
//        } else {
//            return MissingValueStrategy.optional;
//        }
    }
}
