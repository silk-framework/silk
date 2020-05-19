import React from "react";
import { Accordion, AccordionItem, TitleSubsection } from "@wrappers/index";

export function AdvancedOptionsArea({ children, open = false, ...otherProps }: any) {
    return (
        <Accordion>
            <AccordionItem title={<TitleSubsection>Advanced options</TitleSubsection>} fullWidth elevated open={open}>
                {children}
            </AccordionItem>
        </Accordion>
    );
}
