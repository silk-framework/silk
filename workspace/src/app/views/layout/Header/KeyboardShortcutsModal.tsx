import React from "react";
import {
    Button,
    Grid,
    GridColumn,
    GridRow,
    Menu,
    MenuItem,
    OverflowText,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    SimpleDialog,
    Spacing,
    Tag,
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

const sectionKeys = ["general", "rule-editors", "workflow-editor"] as const;
const shortcuts: Record<typeof sectionKeys[number], Array<{ key: string; commands: string[] }>> = {
    general: [{ key: "quick-search", commands: ["/"] }],
    "rule-editors": [
        { key: "duplicate-nodes", commands: ["mod+d", "ctrl+d"] },
        {
            key: "undo",
            commands: ["mod+z", "ctrl+z"],
        },
        {
            key: "redo",
            commands: ["mod+shift+z", "ctrl+shift+z"],
        },
        { key: "delete", commands: ["DELETE"] },
        { key: "select-nodes", commands: ["ALT+mouse select"] },
    ],
    "workflow-editor": [{ key: "delete", commands: ["DELETE"] }],
};

export const KeyboardShortcutsModal: React.FC<KeyboardShortcutsModalProps> = ({ isOpen, onClose, openModal }) => {
    const [t] = useTranslation();
    const [activeSection, setActiveAction] = React.useState<typeof sectionKeys[number]>("general");
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    useHotKey({
        hotkey: hotKeys.keyboardShortcuts,
        handler: () => {
            openModal();
            return false; // prevent default
        },
    });

    const closeModal = React.useCallback(() => {
        setActiveAction("general");
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
            <Grid>
                <GridRow>
                    <GridColumn small>
                        <TitleSubsection>{t("header.keyboardShortcutsModal.categories-title")}</TitleSubsection>
                        <Menu>
                            {sectionKeys.map((sectionKey) => (
                                <MenuItem
                                    key={sectionKey}
                                    text={t(`header.keyboardShortcutsModal.categories.${sectionKey}.label`)}
                                    onClick={() => setActiveAction(sectionKey)}
                                    active={sectionKey === activeSection}
                                />
                            ))}
                        </Menu>
                    </GridColumn>
                    <Spacing size="small" vertical />
                    <GridColumn>
                        <OverviewItemList hasDivider>
                            {shortcuts[activeSection].map((shortcut, i) => (
                                <PropertyValuePair
                                    style={{ width: "100%" }}
                                    hasSpacing
                                    key={activeSection + shortcut.key}
                                >
                                    <PropertyName size="large">
                                        <Tooltip
                                            content={t(
                                                `header.keyboardShortcutsModal.categories.${activeSection}.shortcuts.${shortcut.key}`
                                            )}
                                        >
                                            <OverflowText passDown>
                                                {t(
                                                    `header.keyboardShortcutsModal.categories.${activeSection}.shortcuts.${shortcut.key}`
                                                )}
                                            </OverflowText>
                                        </Tooltip>
                                    </PropertyName>
                                    <PropertyValue
                                        style={{
                                            marginLeft: "calc(31.25% + 14px)",
                                        }}
                                    >
                                        {shortcut.commands.map((command) => (
                                            <React.Fragment key={command}>
                                                <Tag key={command}>{command}</Tag>
                                                <Spacing size="small" vertical />
                                            </React.Fragment>
                                        ))}
                                    </PropertyValue>
                                </PropertyValuePair>
                            ))}
                        </OverviewItemList>
                    </GridColumn>
                </GridRow>
            </Grid>
        </SimpleDialog>
    );
};
