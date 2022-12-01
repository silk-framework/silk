import _ from "lodash";
import React from "react";

import { ThingName } from "./ThingName";

export const ParentElement = ({ parent, ...otherProps }) =>
    _.get(parent, "type") ? (
        <ThingName id={parent.type} {...otherProps} />
    ) : (
        <span {...otherProps}>parent element</span>
    );
