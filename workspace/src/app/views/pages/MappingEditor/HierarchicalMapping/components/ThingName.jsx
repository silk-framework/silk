import React from "react";

import { URIInfo } from "./URIInfo";

export const ThingName = ({ id, ...otherProps }) => <URIInfo uri={id} {...otherProps} field="label" />;
