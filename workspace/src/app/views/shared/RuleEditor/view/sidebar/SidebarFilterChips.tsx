import React from "react";
import { useTranslation } from "react-i18next";
import { FilterChips, Icon } from "@eccenca/gui-elements";

import { IRuleSideBarFilterTabConfig, IRuleSidebarPreConfiguredOperatorsTabConfig } from "../../RuleEditor.typings";
import ruleEditorUtils from "../../RuleEditor.utils";

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
        <FilterChips
            data-test-id="rule-editor-sidebar-tabs"
            selectedChipId={activeTabId}
            onChange={onChange}
            chips={tabs.map((tab) => ({
                id: tab.id,
                label: t("RuleEditor.sidebar.tab." + tab.id, tab.label),
                icon: tab.icon ? <Icon name={tab.icon} small /> : undefined,
                activeColor: getTabColor(tab.id),
            }))}
        />
    );
};
