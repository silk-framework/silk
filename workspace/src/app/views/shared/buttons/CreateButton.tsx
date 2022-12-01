import { Button } from "@eccenca/gui-elements";
import React, { memo } from "react";
import { useTranslation } from "react-i18next";

const CreateButton = memo<any>((props) => {
    const [t] = useTranslation();

    return (
        <Button
            data-test-id="create-item-btn"
            elevated
            text={t("common.action.create", "Create")}
            rightIcon="item-add-artefact"
            {...props}
        />
    );
});

export default CreateButton;
