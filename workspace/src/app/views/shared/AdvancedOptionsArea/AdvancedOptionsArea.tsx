import { Accordion, AccordionItem, TitleSubsection } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

export function AdvancedOptionsArea({ children, open = false, ...otherProps }: any) {
    const [t] = useTranslation();

    return (
        <Accordion data-test-id="advanced-option-accordion">
            <AccordionItem
                label={<TitleSubsection>{t("common.words.advancedOptions", "Advanced options")}</TitleSubsection>}
                fullWidth
                elevated
                open={open}
            >
                {children}
            </AccordionItem>
        </Accordion>
    );
}
