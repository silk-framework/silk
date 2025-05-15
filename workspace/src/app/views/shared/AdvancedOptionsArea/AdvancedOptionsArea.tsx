import React from "react";
import { Accordion, AccordionItem, TitleSubsection, WhiteSpaceContainer } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface AdvancedOptionsAreaProps {
    children: any;
    open?: boolean;
    compact?: boolean;
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
                whitespaceSize={"small"}
                open={open}
            >
                <WhiteSpaceContainer marginTop="small">{children}</WhiteSpaceContainer>
            </AccordionItem>
        </Accordion>
    );
}
