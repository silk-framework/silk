import React from "react";
import { Tag, TagList, Icon, Button, FieldItem } from "@eccenca/gui-elements/src";
import getColorConfiguration from "@eccenca/gui-elements/src/common/utils/getColorConfiguration";
import { CodeEditor } from "@eccenca/gui-elements/src/extensions";
import { RuleEditorBaseModal } from "./RuleEditorBaseModal";

export type StickyNoteModalTranslationKeys = "modalTitle" | "noteLabel" | "colorLabel" | "saveButton" | "cancelButton";

export type StickyNoteMetadataType = { note: string; color: string };

export interface StickyNoteModalProps {
    /**
     * sticky data containing the sticky note and the selected color
     */
    metaData?: StickyNoteMetadataType;
    /**
     * utility to close the sticky note modal when cancelled as well as closed also
     */
    onClose: () => void;
    /**
     * utility to save recently entered metadata for sticky
     *  note and add on to the canvas
     */
    onSubmit: (data: StickyNoteMetadataType) => void;
    /**
     * translation utility for language compatibility
     */
    translate: (key: StickyNoteModalTranslationKeys) => string;
}

export const StickyNoteModal: React.FC<StickyNoteModalProps> = ({ metaData, onClose, onSubmit, translate }) => {
    const refNote = React.useRef<string>(metaData?.note ?? "");
    const [color, setSelectedColor] = React.useState<string>(metaData?.color ?? "");
    const noteColors: [string, string][] = Object.entries(getColorConfiguration("stickynotes")).map(([key, value]) => [
        key,
        value as string,
    ]);

    React.useEffect(() => {
        if (!color && noteColors[0][1]) {
            setSelectedColor(noteColors[0][1]);
        }
    }, [color, noteColors]);

    const predefinedColorsMenu = (
        <TagList>
            {noteColors &&
                noteColors.map(([colorName, colorValue]) => {
                    const selectedFeedback =
                        color === colorValue
                            ? {
                                  icon: <Icon name="state-checkedsimple" />,
                                  large: true,
                              }
                            : {};
                    return (
                        <Tag
                            round
                            onClick={() => setSelectedColor(colorValue)}
                            backgroundColor={colorValue}
                            {...selectedFeedback}
                            key={colorName}
                        />
                    );
                })}
        </TagList>
    );

    return (
        <RuleEditorBaseModal
            data-test-id={"sticky-note-modal"}
            size="small"
            title={translate("modalTitle")}
            hasBorder
            isOpen
            onClose={onClose}
            actions={[
                <Button
                    key="submit"
                    data-test-id="sticky-submit-btn"
                    affirmative
                    onClick={() => {
                        onSubmit({ note: refNote.current, color: color || "#444444" });
                        onClose();
                    }}
                >
                    {translate("saveButton")}
                </Button>,
                <Button key="cancel" onClick={onClose}>
                    {translate("cancelButton")}
                </Button>,
            ]}
        >
            <FieldItem
                key="note"
                labelProps={{
                    htmlFor: "noteinput",
                    text: translate("noteLabel"),
                }}
            >
                <CodeEditor
                    name={translate("noteLabel")}
                    id={"sticky-note-input"}
                    mode="markdown"
                    preventLineNumbers
                    onChange={(value) => {
                        refNote.current = value;
                    }}
                    defaultValue={refNote.current}
                />
            </FieldItem>
            <FieldItem
                key="color"
                labelProps={{
                    htmlFor: "colorinput",
                    text: translate("colorLabel"),
                }}
            >
                {predefinedColorsMenu}
            </FieldItem>
        </RuleEditorBaseModal>
    );
};
