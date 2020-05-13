import React from "react";
import { AccordionItem as CarbonAccordionItem } from "carbon-components-react";

function AccordionItem({ children, className = "", fullWidth = false, elevated = false, ...otherProps }: any) {
    return (
        <CarbonAccordionItem
            className={
                "ecc-accordion__item" +
                (className ? " " + className : "") +
                (fullWidth ? " ecc-accordion__item--fullwidth" : "") +
                (elevated ? " ecc-accordion__item--elevated" : "")
            }
            {...otherProps}
        >
            {children}
        </CarbonAccordionItem>
    );
}

export default AccordionItem;
