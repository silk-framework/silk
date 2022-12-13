import { FieldItem, TextField } from "@eccenca/gui-elements";
import { URI } from "ecc-utils";
import React from "react";
import { useTranslation } from "react-i18next";

import { valueIsIRI } from "../../../../pages/MappingEditor/HierarchicalMapping/utils/newValueIsIRI";

export const URI_PROPERTY_PARAMETER_ID = "uriProperty";

interface Props {
    initialValue?: string;
    onValueChange: (event: any) => any;
}

/** Input element for the URI attribute. */
export const UriAttributeParameterInput = ({ onValueChange, initialValue }: Props) => {
    const [t] = useTranslation();
    const [validationFailed, setValidationFailed] = React.useState(false);

    const onChange: React.FormEventHandler<HTMLElement> = (e: any) => {
        const value = (e.target ? e.target.value : e).trim();
        if (value && !valueIsIRI(value)) {
            setValidationFailed(true);
        } else {
            setValidationFailed(false);
        }
        // Use same logic as in the mapping editor to generate valid URIs
        onValueChange(new URI(value).normalize().toString());
    };

    return (
        <FieldItem
            labelProps={{
                text: t("DatasetUriPropertyParameter.label"),
                htmlFor: URI_PROPERTY_PARAMETER_ID,
            }}
            helperText={t("DatasetUriPropertyParameter.description")}
            hasStateDanger={validationFailed}
            messageText={validationFailed ? t("DatasetUriPropertyParameter.invalid") : undefined}
        >
            <TextField
                id={URI_PROPERTY_PARAMETER_ID}
                name={URI_PROPERTY_PARAMETER_ID}
                onChange={onChange}
                hasStateDanger={validationFailed}
                defaultValue={initialValue}
            />
        </FieldItem>
    );
};
