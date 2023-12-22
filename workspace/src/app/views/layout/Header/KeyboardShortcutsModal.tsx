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

const sectionKeys = ["general", "workflow-editor", "rule-editors", "projects", "tasks"] as const;
const shortcuts: Record<typeof sectionKeys[number], Array<{ key: string; commands: string[] }>> = {
    general: [
        { key: "quick-search", commands: ["/"] },
        { key: "help", commands: ["?"] },
        { key: "go-home", commands: ["g", "*then", "h"] },
        { key: "browse-projects", commands: ["g", "*then", "p"] },
        { key: "browse-datasets", commands: ["g", "*then", "d"] },
        { key: "browse-workflows", commands: ["g", "*then", "w"] },
        { key: "browse-transform-tasks", commands: ["g", "*then", "t"] },
        { key: "browse-linking-tasks", commands: ["g", "*then", "l"] },
        { key: "browse-tasks", commands: ["g", "*then", "o"] },
        { key: "browse-activities-tasks", commands: ["g", "*then", "a"] },
        { key: "create-project", commands: ["c", "*then", "p"] },
        { key: "create-workflow", commands: ["c", "*then", "w"] },
        { key: "create-dataset", commands: ["c", "*then", "d"] },
        { key: "create-transform", commands: ["c", "*then", "t"] },
        { key: "create-linking", commands: ["c", "*then", "l"] },
        { key: "create-task", commands: ["c", "*then", "o"] },
        { key: "create-new-item", commands: ["c", "*then", "n"] },
    ],
    projects: [
        { key: "manage-prefixes", commands: ["e", "*then", "p"] },
        { key: "edit-summary", commands: ["e", "*then", "s"] },
    ],
    tasks: [
        { key: "update-tasks", commands: ["e", "*then", "c"] },
        { key: "edit-summary", commands: ["e", "*then", "s"] },
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
        { key: "create-dataset", commands: ["c", "*then", "d"] },
        { key: "create-transform", commands: ["c", "*then", "t"] },
        { key: "create-linking", commands: ["c", "*then", "l"] },
        { key: "create-task", commands: ["c", "*then", "o"] },
        { key: "create-new-item", commands: ["c", "*then", "n"] },
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
            forceTopPosition
        >
            <OverviewItemList hasDivider columns={1}>
                {sectionKeys.map((sectionKey) => (
                    <section key={sectionKey} style={{ margin: "0.5em 0" }}>
                        <TitleSubsection>
                            {t(`header.keyboardShortcutsModal.categories.${sectionKey}.label`)}
                        </TitleSubsection>
                        <OverviewItemList densityHigh columns={2}>
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
                                            {shortcut.commands.map((command, i) => {
                                                const keyDirective = command.replace("*", "");
                                                return command.startsWith("*") ? (
                                                    <React.Fragment key={command + i}>
                                                        {" "}
                                                        <p>
                                                            {t(
                                                                `header.keyboardShortcutsModal.key-directives.${keyDirective}`
                                                            )}
                                                        </p>
                                                    </React.Fragment>
                                                ) : (
                                                    <Tag key={command + i}>
                                                        {command
                                                            .split("+")
                                                            .map((key) => {
                                                                return t(
                                                                    `header.keyboardShortcutsModal.keys.${key}`,
                                                                    key
                                                                );
                                                            })
                                                            .join(" + ")}
                                                    </Tag>
                                                );
                                            })}
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
