import {Button, SimpleDialog, FieldItem, TextField} from "@eccenca/gui-elements"
import React from "react";
import {useTranslation} from "react-i18next";

interface Props {
    link: {
        source: string;
        target: string;
    }
    onClose: () => any
}

/** Shows the entity URIs of a reference link. */
export const ReferenceLinkEntityUrisModal = ({link, onClose}: Props) => {
    const [t] = useTranslation()

    return <SimpleDialog
        title={t("ReferenceLinks.showEntityUris.modalTitle")}
        isOpen={true}
        size={"small"}
        onClose={onClose}
        actions={<Button onClick={onClose}>{t("common.action.close")}</Button>}
    >
        <FieldItem
            labelProps={{
                text: t("ReferenceLinks.showEntityUris.sourceEntity"),

            }}
        >
            <TextField readOnly={true} defaultValue={link.source} />
        </FieldItem>
        <FieldItem
            labelProps={{
                text: t("ReferenceLinks.showEntityUris.targetEntity")
            }}
        >
            <TextField readOnly={true} defaultValue={link.target} />
        </FieldItem>
    </SimpleDialog>
}
