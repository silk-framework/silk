import React from "react";
import { useTranslation } from "react-i18next";
import { Icon, shadcn } from "@eccenca/gui-elements";
import decideContrastColorValue from "@eccenca/gui-elements/src/common/utils/colorDecideContrastvalue";
import Color from "color";

import { IRuleSideBarFilterTabConfig, IRuleSidebarPreConfiguredOperatorsTabConfig } from "../../RuleEditor.typings";
import ruleEditorUtils from "../../RuleEditor.utils";

const { ToggleGroup, ToggleGroupItem } = shadcn;

interface SidebarFilterChipsProps {
    /** The tab configs of the hosting rule editor (order defines chip order). */
    tabs: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Currently active tab/filter id. */
    activeTabId: string;
    /** Called with the newly selected tab id. */
    onChange: (tabId: string) => void;
}

/**
 * Single-select, wrapping filter chips for the operator sidebar. Replaces the former
 * icon-only tab strip: every filter shows its label (wrapping onto further rows when the
 * sidebar is narrow), and the active chip is tinted with the operator-type color so it
 * matches the tag pills on the operator cards and the node headers on the canvas.
 */
export const SidebarFilterChips = ({ tabs, activeTabId, onChange }: SidebarFilterChipsProps) => {
    const [t] = useTranslation();
    const getTabColor = ruleEditorUtils.linkingRuleOperatorTypeColorFunction();

    return (
        <ToggleGroup
            type="single"
            value={activeTabId}
            // Radix allows deselecting the active item (empty value); exactly one filter
            // must stay active, so ignore that case.
            onValueChange={(value: string) => {
                if (value) onChange(value);
            }}
            data-test-id="rule-editor-sidebar-tabs"
            className="w-full flex-wrap justify-start gap-1"
        >
            {tabs.map((tab) => {
                const color = tab.id === activeTabId ? getTabColor(tab.id) : undefined;
                // The active chip is filled with the operator-type color; the readable text
                // color is derived the same way the Tag pills do it. Inactive chips stay
                // uniform outlines (no inline style).
                let activeStyle: React.CSSProperties | undefined;
                if (color) {
                    try {
                        const bg = Color(color).rgb().toString();
                        activeStyle = {
                            backgroundColor: bg,
                            borderColor: bg,
                            color: decideContrastColorValue({ testColor: color }),
                        };
                    } catch {
                        // invalid color configuration: fall back to the neutral active style
                    }
                }
                return (
                    <ToggleGroupItem
                        key={tab.id}
                        value={tab.id}
                        style={activeStyle}
                        className={
                            "h-6 min-w-0 gap-1 rounded-md border border-input bg-background px-2 text-xs font-medium text-muted-foreground transition-colors " +
                            "outline-none hover:bg-accent/70 hover:text-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 " +
                            "data-[state=on]:border-transparent data-[state=on]:bg-secondary data-[state=on]:text-secondary-foreground"
                        }
                    >
                        {tab.icon ? <Icon name={tab.icon} small /> : null}
                        {t("RuleEditor.sidebar.tab." + tab.id, tab.label)}
                    </ToggleGroupItem>
                );
            })}
        </ToggleGroup>
    );
};
