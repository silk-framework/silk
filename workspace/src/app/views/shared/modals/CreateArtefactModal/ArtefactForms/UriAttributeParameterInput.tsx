import { useTranslation } from "react-i18next";
import React from "react";
import { TextField } from "@eccenca/gui-elements";
import { valueIsIRI } from "../../../../pages/MappingEditor/HierarchicalMapping/utils/newValueIsIRI";
import { URI } from "ecc-utils";
import { ArtefactFormParameter } from "./ArtefactFormParameter";

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
        <ArtefactFormParameter
            parameterId={URI_PROPERTY_PARAMETER_ID}
            label={t("DatasetUriPropertyParameter.label")}
            helperText={t("DatasetUriPropertyParameter.description")}
            infoMessage={validationFailed ? t("DatasetUriPropertyParameter.invalid") : undefined}
            inputElementFactory={() => (
                <TextField
                    id={URI_PROPERTY_PARAMETER_ID}
                    name={URI_PROPERTY_PARAMETER_ID}
                    onChange={onChange}
                    hasStateDanger={validationFailed}
                    defaultValue={initialValue}
                    escapeToBlur={true}
                />
            )}
        />
    );
};
