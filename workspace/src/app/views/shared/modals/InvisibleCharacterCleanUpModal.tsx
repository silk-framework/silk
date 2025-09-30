import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { Button, IconButton, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import chars from "@eccenca/gui-elements/src/common/utils/characters";
import { InvisibleCharacterWarningProps } from "@eccenca/gui-elements/src/components/TextField/useTextValidation";

interface Props {
    /** The code points of the invisible characters that were detected. */
    detectedCodePoints: Set<number>;
    /** The string that was input into the input field.*/
    inputString: string;
    /** Optional title for the clean up modal. */
    title?: string;
    /** Callback for the cleaned string if the 'Clean text' action was triggered. */
    setString: (cleanedString: string) => any;
    /** Function to call when the modal should be cloesd. */
    onClose: () => any;
    /** If the modal should be closed after executing the clear operation. Default: true */
    closeAfterClear?: boolean;
}

/** Modal that offers the user to clean up the given input string. Use the hook instead of the component directly. */
export const InvisibleCharacterCleanUpModal = ({
    inputString,
    title,
    setString,
    onClose,
    detectedCodePoints,
    closeAfterClear = true,
}: Props) => {
    const [t] = useTranslation();

    const charMap = chars.invisibleZeroWidthCharacters.codePointMap;
    const detectedChars = [...detectedCodePoints].map((cp) => charMap.get(cp)).filter((cp) => cp != null);
    const onClean = () => {
        setString(chars.invisibleZeroWidthCharacters.clearString(inputString));
        if (closeAfterClear) {
            onClose();
        }
    };

    return (
        <SimpleDialog
            data-test-id={"invisible-character-cleanup-modal"}
            title={title ?? t("InvisibleCharacterHandling.modal.title")}
            onClose={onClose}
            isOpen={true}
            actions={[
                <Button
                    key="clean"
                    data-test-id={"clean-invisible-characters"}
                    disruptive
                    onClick={onClean}
                    tooltip={t("InvisibleCharacterHandling.modal.cleanActionTooltip")}
                >
                    {t("InvisibleCharacterHandling.modal.cleanAction")}
                </Button>,
                <Button key="close" onClick={onClose}>
                    {t("common.action.close", "Close")}
                </Button>,
            ]}
        >
            <h3>{t("InvisibleCharacterHandling.modal.description")}</h3>
            <Spacing />
            <ul>
                {detectedChars.map((char) => {
                    return <li key={char!!.codePoint}>{char!!.fullLabel}</li>;
                })}
            </ul>
        </SimpleDialog>
    );
};

interface HookProps extends Omit<Props, "onClose" | "detectedCodePoints"> {
    /** The delay before an input value is checked for invisible characters. Only the most recent value will be checked. */
    callbackDelay?: number;
}

/** Creates the invisibleCharacterWarning object for the input components, e.g. TextField, the warning (action) icon and clean up modal.
 * Also handles the clean up modal logic. */
export const useInvisibleCharacterCleanUpModal = ({
    inputString,
    title,
    setString,
    callbackDelay,
}: HookProps): HookResult => {
    const [isOpen, setIsOpen] = React.useState(false);
    const [detectedCodePoints, setDetectedCodePoints] = useState<Set<number>>(new Set());
    const [t] = useTranslation();

    const invisibleCharacterWarningCallback = React.useCallback((detectedCodePoints: Set<number>) => {
        setDetectedCodePoints((old) => (old.size === 0 && detectedCodePoints.size === 0 ? old : detectedCodePoints));
    }, []);

    const invisibleCharacterWarning: InvisibleCharacterWarningProps = React.useMemo(() => {
        return {
            callback: invisibleCharacterWarningCallback,
            callbackDelay,
        };
    }, [invisibleCharacterWarningCallback, callbackDelay]);

    const openModal = React.useCallback(() => setIsOpen(true), []);

    const resetCleanUpModalComponent = React.useCallback(() => setDetectedCodePoints(new Set()), []);

    return {
        invisibleCharacterWarning,
        resetCleanUpModalComponent,
        modalElement:
            isOpen && detectedCodePoints.size ? (
                <InvisibleCharacterCleanUpModal
                    title={title}
                    inputString={inputString}
                    detectedCodePoints={detectedCodePoints}
                    setString={setString}
                    onClose={() => setIsOpen(false)}
                />
            ) : null,
        iconButton: detectedCodePoints.size ? (
            <IconButton
                data-test-id={"invisible-character-warning"}
                hasStateWarning={true}
                onClick={openModal}
                name={"state-warning"}
                text={t("InvisibleCharacterHandling.iconTooltip")}
            />
        ) : undefined,
    };
};

interface HookResult {
    /** Standard IconButton element that can be clicked to open the modal. */
    iconButton?: React.JSX.Element;
    /** The modal that should be displayed. */
    modalElement: React.JSX.Element | null;
    /** The object that should be forwarded to the input element. */
    invisibleCharacterWarning?: InvisibleCharacterWarningProps;
    /** Allows to reset this component, i.e. resetting the detected characters. */
    resetCleanUpModalComponent: () => any;
}
