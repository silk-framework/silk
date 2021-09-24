import React from "react";
import { Accordion, AccordionItem, TitleSubsection } from "@gui-elements/index";
import { useTranslation } from "react-i18next";

export function AdvancedOptionsArea({ children, open = false, ...otherProps }: any) {
    const [t] = useTranslation();

    return (
        <Accordion>
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
