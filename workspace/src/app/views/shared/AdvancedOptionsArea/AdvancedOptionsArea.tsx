import React from "react";
import { Accordion, AccordionItem, TitleSubsection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import {compact} from "lodash";

interface AdvancedOptionsAreaProps {
    children: any
    open?: boolean
    compact?: boolean
}
export function AdvancedOptionsArea({ children, open = false, compact = false }: AdvancedOptionsAreaProps) {
    const [t] = useTranslation();

    return (
        <Accordion data-test-id="advanced-option-accordion">
            <AccordionItem
                label={<TitleSubsection>{t("common.words.advancedOptions", "Advanced options")}</TitleSubsection>}
                fullWidth
                elevated
                noBorder={compact}
                condensed={compact}
                open={open}
            >
                {children}
            </AccordionItem>
        </Accordion>
    );
}
