import React from "react";
import { Accordion as CarbonAccordion } from "carbon-components-react";

function Accordion({ children, className = "", align = "start", ...otherProps }: any) {
    return (
        <CarbonAccordion className={"ecc-accordion__container " + className} align={align} {...otherProps}>
            {children}
        </CarbonAccordion>
    );
}

export default Accordion;
