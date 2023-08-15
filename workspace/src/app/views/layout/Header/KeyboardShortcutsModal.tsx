import React from "react";
import {
    Button,
    OverflowText,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    SimpleDialog,
    Tag,
    TagList,
    TitleSubsection,
    Tooltip,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import useHotKey from "../../../views/shared/HotKeyHandler/HotKeyHandler";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

interface KeyboardShortcutsModalProps {
    isOpen: boolean;
    onClose: () => void;
    openModal: () => void;
}

const sectionKeys = ["general", "workflow-editor", "rule-editors"] as const;
const shortcuts: Record<typeof sectionKeys[number], Array<{ key: string; commands: string[] }>> = {
    general: [{ key: "quick-search", commands: ["/"] }],
    "rule-editors": [
        { key: "duplicate-nodes", commands: ["ctrl+d", "mod+d"] },
        {
            key: "undo",
            commands: ["ctrl+z", "mod+z"],
        },
        {
            key: "redo",
            commands: ["ctrl+shift+z", "mod+shift+z"],
        },
        { key: "delete", commands: ["backspace"] },
        { key: "select-nodes", commands: ["alt+mouse select"] },
    ],
    "workflow-editor": [
        { key: "delete", commands: ["backspace"] },
        { key: "select-nodes", commands: ["alt+mouse select"] },
    ],
};

export const KeyboardShortcutsModal: React.FC<KeyboardShortcutsModalProps> = ({ isOpen, onClose, openModal }) => {
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    useHotKey({
        hotkey: hotKeys.keyboardShortcuts,
        handler: () => {
            openModal();
            return false; // prevent default
        },
    });

    const closeModal = React.useCallback(() => {
        onClose();
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
            <OverviewItemList columns={2} hasDivider hasSpacing>
                {sectionKeys.map((sectionKey) => (
                    <section>
                        <TitleSubsection>
                            {t(`header.keyboardShortcutsModal.categories.${sectionKey}.label`)}
                        </TitleSubsection>
                        <OverviewItemList densityHigh>
                            {shortcuts[sectionKey].map((shortcut, i) => (
                                <PropertyValuePair style={{ width: "100%" }} hasSpacing key={sectionKey + shortcut.key}>
                                    <PropertyName size="large">
                                        <Tooltip
                                            content={t(
                                                `header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}`
                                            )}
                                        >
                                            <OverflowText passDown>
                                                {t(
                                                    `header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}`
                                                )}
                                            </OverflowText>
                                        </Tooltip>
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
                    </section>
                ))}
            </OverviewItemList>
        </SimpleDialog>
    );
};
