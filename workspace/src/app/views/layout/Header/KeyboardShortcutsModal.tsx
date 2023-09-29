import React from "react";
import {
    Button,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    SimpleDialog,
    Tag,
    TagList,
    TitleSubsection,
    Spacing,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import useHotKey from "../../../views/shared/HotKeyHandler/HotKeyHandler";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

const sectionKeys = ["general", "workflow-editor", "rule-editors"] as const;
const shortcuts: Record<typeof sectionKeys[number], Array<{ key: string; commands: string[] }>> = {
    general: [
        { key: "quick-search", commands: ["/"] },
        { key: "help", commands: ["?"] }
    ],
    "rule-editors": [
        { key: "duplicate-nodes", commands: ["ctrl+d", "cmd+d"] },
        {
            key: "undo",
            commands: ["ctrl+z", "cmd+z"],
        },
        {
            key: "redo",
            commands: ["ctrl+shift+z", "cmd+shift+z"],
        },
        { key: "delete", commands: ["backspace"] },
        { key: "multiselect", commands: ["shift+mouse select"] },
    ],
    "workflow-editor": [
        { key: "delete", commands: ["backspace"] },
        { key: "multiselect", commands: ["shift+mouse select"] },
    ],
};

export const KeyboardShortcutsModal = () => {
    const [isOpen, setIsOpen] = React.useState<boolean>(false);
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    useHotKey({
        hotkey: hotKeys.overview,
        handler: () => {
            setIsOpen(true);
            return false; // prevent default
        },
    });

    const closeModal = React.useCallback(() => {
        setIsOpen(false);
    }, []);

    return (
        <SimpleDialog
            data-test-id="keyboard-shortcuts"
            size="large"
            title={t("header.keyboardShortcutsModal.title")}
            isOpen={isOpen}
            onClose={closeModal}
            actions={[
                <Button key="cancel" onClick={closeModal}>
                    {t("common.action.close", "Close")}
                </Button>,
            ]}
        >
            <OverviewItemList columns={1}>
                {sectionKeys.map((sectionKey) => (
                    <section key={sectionKey}>
                        <TitleSubsection>
                            {t(`header.keyboardShortcutsModal.categories.${sectionKey}.label`)}
                        </TitleSubsection>
                        <OverviewItemList densityHigh hasDivider>
                            {shortcuts[sectionKey].map((shortcut, i) => (
                                <PropertyValuePair style={{ width: "100%" }} hasSpacing key={sectionKey + shortcut.key}>
                                    <PropertyName
                                        size="large"
                                        labelProps={{
                                            tooltip: t(
                                                `header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}Desc`,
                                                ""
                                            ),
                                        }}
                                    >
                                        {t(
                                            `header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}`
                                        )}
                                    </PropertyName>
                                    <PropertyValue
                                        style={{
                                            marginLeft: "calc(31.25% + 14px)",
                                        }}
                                    >
                                        <TagList>
                                            {shortcut.commands.map((command) => (
                                                <Tag key={command}>
                                                    {command
                                                        .split("+")
                                                        .map((key) => {
                                                            return t(`header.keyboardShortcutsModal.keys.${key}`, key);
                                                        })
                                                        .join(" + ")}
                                                </Tag>
                                            ))}
                                        </TagList>
                                    </PropertyValue>
                                </PropertyValuePair>
                            ))}
                        </OverviewItemList>
                        <Spacing style={{ clear: "both" }} />
                    </section>
                ))}
            </OverviewItemList>
        </SimpleDialog>
    );
};
