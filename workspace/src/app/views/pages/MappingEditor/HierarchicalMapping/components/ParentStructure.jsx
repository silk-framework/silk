import _ from "lodash";
import React from "react";

import { ParentElement } from "./ParentElement";
import { ThingName } from "./ThingName";

export const ParentStructure = ({ parent, ...otherProps }) =>
    _.get(parent, "property") ? (
        <ThingName id={parent.property} {...otherProps} />
    ) : (
        <ParentElement parent={parent} {...otherProps} />
    );
