import React from "react";
import { Button, SimpleDialog, shadcn } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import useHotKey from "../../../views/shared/HotKeyHandler/HotKeyHandler";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { artefactTypes } from "./artefactTypes";

const { Kbd, KbdGroup } = shadcn;

const sectionKeys = ["general", "workflow-editor", "rule-editors", "projects", "tasks"] as const;
type SectionKey = (typeof sectionKeys)[number];

// "c <suffix>" create-chord rows sourced from the shared artefact-type registry so the documented
// suffixes can't drift from the actual bindings. `key` maps to the existing
// `...categories.<section>.shortcuts.create-<dtype>` i18n label.
const createShortcutRow = (dtype: string): { key: string; commands: string[] } => {
    const type = artefactTypes.find((a) => a.dtype === dtype)!;
    return { key: `create-${type.dtype}`, commands: ["c", "*then", type.hotkeySuffix] };
};

const shortcuts: Record<SectionKey, Array<{ key: string; commands: string[] }>> = {
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
        ...artefactTypes.map((type) => createShortcutRow(type.dtype)),
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
        { key: "undo", commands: ["ctrl+z", "cmd+z"] },
        { key: "redo", commands: ["ctrl+shift+z", "cmd+shift+z"] },
        { key: "delete", commands: ["backspace"] },
        { key: "multiselect", commands: ["shift+mouse select"] },
        { key: "copySelectedNodes", commands: ["ctrl+c", "cmd+c"] },
        { key: "pasteNodes", commands: ["ctrl+v", "cmd+v"] },
    ],
    "workflow-editor": [
        { key: "delete", commands: ["backspace"] },
        { key: "multiselect", commands: ["shift+mouse select"] },
        // Curated subset of the create chords relevant inside the workflow editor.
        ...["dataset", "transform", "linking", "task"].map(createShortcutRow),
        { key: "create-new-item", commands: ["c", "*then", "n"] },
    ],
};

/** Renders a single key combination (e.g. `ctrl+d`) as a group of individual keycaps. */
const KeyCombo = ({ combo }: { combo: string }) => {
    const [t] = useTranslation();
    return (
        <KbdGroup>
            {combo.split("+").map((key, i) => (
                <React.Fragment key={key + i}>
                    {i > 0 && <span className="text-xs text-muted-foreground">+</span>}
                    <Kbd>{t(`header.keyboardShortcutsModal.keys.${key}`, key)}</Kbd>
                </React.Fragment>
            ))}
        </KbdGroup>
    );
};

const ShortcutRow = ({
    sectionKey,
    shortcut,
}: {
    sectionKey: SectionKey;
    shortcut: { key: string; commands: string[] };
}) => {
    const [t] = useTranslation();
    const description = t(`header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}Desc`, "");
    return (
        <div className="flex items-baseline justify-between gap-6 py-1.5" title={description || undefined}>
            <span className="text-sm text-foreground">
                {t(`header.keyboardShortcutsModal.categories.${sectionKey}.shortcuts.${shortcut.key}`)}
            </span>
            <div className="flex shrink-0 flex-wrap items-center justify-end gap-1.5">
                {shortcut.commands.map((command, i) =>
                    command.startsWith("*") ? (
                        <span key={command + i} className="text-xs italic text-muted-foreground">
                            {t(`header.keyboardShortcutsModal.key-directives.${command.replace("*", "")}`)}
                        </span>
                    ) : (
                        <KeyCombo key={command + i} combo={command} />
                    ),
                )}
            </div>
        </div>
    );
};

export const KeyboardShortcutsModal = () => {
    const [isOpen, setIsOpen] = React.useState<boolean>(false);
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    // The "quick search" and "help" (this modal's own) shortcuts are user-configurable
    // (see `hotKeys` in initial settings, rendered the same way in `Header.tsx`), so their
    // keycaps must reflect the actual configured binding instead of the historical "/" and "?".
    const sections: Record<SectionKey, Array<{ key: string; commands: string[] }>> = {
        ...shortcuts,
        general: shortcuts.general.map((shortcut) => {
            if (shortcut.key === "quick-search") {
                return { ...shortcut, commands: [hotKeys.quickSearch ?? "/"] };
            }
            if (shortcut.key === "help") {
                return { ...shortcut, commands: [hotKeys.overview ?? "?"] };
            }
            return shortcut;
        }),
    };

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

    return isOpen ? (
        <SimpleDialog
            data-test-id="keyboard-shortcuts"
            size="large"
            title={t("header.keyboardShortcutsModal.title")}
            isOpen={true}
            onClose={closeModal}
            actions={[
                <Button key="cancel" onClick={closeModal}>
                    {t("common.action.close", "Close")}
                </Button>,
            ]}
            forceTopPosition
        >
            <div className="gap-x-10 md:columns-2">
                {sectionKeys.map((sectionKey) => (
                    <section key={sectionKey} className="mb-6 break-inside-avoid">
                        <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                            {t(`header.keyboardShortcutsModal.categories.${sectionKey}.label`)}
                        </h3>
                        <div className="divide-y divide-border/60">
                            {sections[sectionKey].map((shortcut) => (
                                <ShortcutRow key={shortcut.key} sectionKey={sectionKey} shortcut={shortcut} />
                            ))}
                        </div>
                    </section>
                ))}
            </div>
        </SimpleDialog>
    ) : null;
};
