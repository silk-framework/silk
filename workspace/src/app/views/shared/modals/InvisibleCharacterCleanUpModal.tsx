import { useTranslation } from "react-i18next";
import React from "react";
import { Button, IconButton, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import chars from "@eccenca/gui-elements/src/common/utils/characters";

interface Props {
    inputString: string;
    detectedCodePoints: Set<number>;
    title?: string;
    setString: (cleanedString: string) => any;
    onClose: () => any;
    /** Function that should be called after the clear action has been executed. */
    onClear: () => any;
    /** If the modal should be closed after executing the clear operation. */
    closeAfterClear?: boolean;
}

/** Modal that offers the user to clean up the given input string. Use the hook instead of the component directly. */
export const InvisibleCharacterCleanUpModal = ({
    inputString,
    detectedCodePoints,
    title,
    setString,
    onClose,
    onClear,
    closeAfterClear = true,
}: Props) => {
    const [t] = useTranslation();

    const charMap = chars.invisibleZeroWidthCharacters.codePointMap;
    const detectedChars = [...detectedCodePoints].map((cp) => charMap.get(cp)).filter((cp) => cp != null);
    const onClean = () => {
        setString(chars.invisibleZeroWidthCharacters.clearString(inputString));
        onClear();
        if (closeAfterClear) {
            onClose();
        }
    };

    return (
        <SimpleDialog
            title={title ?? t("InvisibleCharacterHandling.modal.title")}
            onClose={onClose}
            isOpen={true}
            actions={[
                <Button
                    key="clean"
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

export const useInvisibleCharacterCleanUpModal = ({
    inputString,
    detectedCodePoints,
    title,
    onClear,
    setString,
}: Omit<Props, "onClose">): HookResult => {
    const [isOpen, setIsOpen] = React.useState(false);
    const [t] = useTranslation();

    const openModal = React.useCallback(() => setIsOpen(true), []);

    return {
        openModal,
        modalElement:
            isOpen && detectedCodePoints.size ? (
                <InvisibleCharacterCleanUpModal
                    title={title}
                    inputString={inputString}
                    detectedCodePoints={detectedCodePoints}
                    setString={setString}
                    onClear={onClear}
                    onClose={() => setIsOpen(false)}
                />
            ) : null,
        iconButton: detectedCodePoints.size ? (
            <IconButton
                hasStateWarning={true}
                onClick={openModal}
                name={"state-warning"}
                text={t("InvisibleCharacterHandling.iconTooltip")}
            />
        ) : undefined,
    };
};

interface HookResult {
    /** If defined, there were characters found that can be cleaned via the modal. */
    openModal?: () => any;
    /** Standard IconButton element that can be clicked to open the modal. */
    iconButton?: JSX.Element;
    /** The modal that should be displayed. */
    modalElement: JSX.Element | null;
}
