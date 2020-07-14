import React, { memo } from "react";
import { Button } from "@wrappers/index";
import { useTranslation } from "react-i18next";

const CreateButton = memo<any>((props) => {
    const [t] = useTranslation();

    return <Button elevated text={t("common.actions.create", "Create")} rightIcon="item-add-artefact" {...props} />;
});

export default CreateButton;
