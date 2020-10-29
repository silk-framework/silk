import React, { useState } from "react";
import { SimpleDialog } from "@gui-elements/index";
import useHotKey from "../HotKeyHandler/HotKeyHandler";
import { useTranslation } from "react-i18next";

export function RecentlyViewedModal() {
    const [isOpen, setIsOpen] = useState(false);
    const { t } = useTranslation();

    useHotKey({
        hotkey: "ctrl+shift+e",
        handler: () => setIsOpen(true),
    });
    return (
        <SimpleDialog
            transitionDuration={20}
            onClose={() => setIsOpen(false)}
            isOpen={isOpen}
            title={t("RecentlyViewedModal.title")}
        >
            What?
        </SimpleDialog>
    );
}
