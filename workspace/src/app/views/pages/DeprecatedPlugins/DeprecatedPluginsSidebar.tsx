import React from "react";
import { Label, RadioButton, Spacing, TitleSubsection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { PluginGroup } from "./index";

interface DeprecatedPluginsSidebarProps {
    pluginGroups: PluginGroup[];
    selectedPluginKey: string | null;
    onSelectPlugin: (pluginId: string) => void;
}

export function DeprecatedPluginsSidebar({ pluginGroups, selectedPluginKey, onSelectPlugin }: DeprecatedPluginsSidebarProps) {
    const [t] = useTranslation();

    return (
        <nav>
            <TitleSubsection>
                <Label isLayoutForElement="h3" text={t("pages.deprecatedPlugins.title")} />
            </TitleSubsection>
            <Spacing size="tiny" />
            <ul>
                {pluginGroups.map(({ pluginId, pluginLabel, count }) => (
                    <li key={pluginId}>
                        <RadioButton
                            checked={selectedPluginKey === pluginId}
                            label={`${pluginLabel} (${count})`}
                            onChange={() => onSelectPlugin(pluginId)}
                            value={pluginId}
                        />
                    </li>
                ))}
            </ul>
        </nav>
    );
}
