import React from "react";
import { Card, CardHeader, CardTitle, CardOptions, IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { SERVE_PATH } from "../../../constants/path";

const ActivityInfoWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const projectPath = `projects/${projectId}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`;
    return (
        <Card>
            <CardHeader>
                <CardTitle>{t("widget.ActivityInfoWidget.title", "Activities")}</CardTitle>
                <CardOptions>
                    <IconButton
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            dispatch(routerOp.goToPage(projectPath));
                        }}
                        href={`${SERVE_PATH}/${projectPath}`}
                        data-test-id={"open-project-activities-btn"}
                        name="item-viewdetails"
                        text={t("widget.ActivityInfoWidget.view", "View project activities")}
                    />
                </CardOptions>
            </CardHeader>
        </Card>
    );
};

export default ActivityInfoWidget;
