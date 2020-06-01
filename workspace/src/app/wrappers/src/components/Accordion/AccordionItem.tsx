import React from "react";
import { AccordionItem as CarbonAccordionItem } from "carbon-components-react";

function AccordionItem({
    children,
    className = "",
    fullWidth = false,
    elevated = false,
    condensed = false,
    noBorder = false,
    ...otherProps
}: any) {
    return (
        <CarbonAccordionItem
            className={
                "ecc-accordion__item" +
                (className ? " " + className : "") +
                (fullWidth ? " ecc-accordion__item--fullwidth" : "") +
                (elevated ? " ecc-accordion__item--elevated" : "") +
                (condensed ? " ecc-accordion__item--condensed" : "") +
                (noBorder ? " ecc-accordion__item--noborder" : "")
            }
            {...otherProps}
        >
            {children}
        </CarbonAccordionItem>
    );
}

export default AccordionItem;
