import React from "react";
import { Card, CardHeader, CardTitle, CardOptions, IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { SERVE_PATH } from "../../../constants/path";
import { AppDispatch } from "store/configureStore";

/** Links to the change journal of the project. */
const ChangesWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();

    const changesPath = `projects/${projectId}/changes`;
    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>{t("widget.ChangesWidget.title", "Changes")}</h2>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            dispatch(routerOp.goToPage(changesPath));
                        }}
                        href={`${SERVE_PATH}/${changesPath}`}
                        data-test-id={"open-project-changes-btn"}
                        name="item-viewdetails"
                        text={t("widget.ChangesWidget.view", "View project changes")}
                    />
                </CardOptions>
            </CardHeader>
        </Card>
    );
};

export default ChangesWidget;
