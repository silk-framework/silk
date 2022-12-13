import {
    Button,
    Grid,
    GridColumn,
    GridRow,
    HtmlContentBlock,
    Icon,
    Notification,
    TitleMainsection,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

import { PUBLIC_URL, SERVE_PATH } from "../../../constants/path";

export default function NotFound() {
    const [t] = useTranslation();

    return (
        <Grid data-test-id={"not-found-page"}>
            <GridRow fullHeight>
                <div style={{ margin: "auto", maxWidth: "40rem" }}>
                    <GridColumn verticalAlign={"center"}>
                        <Notification
                            actions={
                                <Button
                                    minimal
                                    outlined
                                    icon={<Icon name="application-homepage" />}
                                    text={t("pages.notFound.backLink", "Go to homepage")}
                                    href={PUBLIC_URL + SERVE_PATH}
                                />
                            }
                        >
                            <HtmlContentBlock>
                                <TitleMainsection>{t("pages.notFound.title", "Page not found.")}</TitleMainsection>
                                <p>{t("pages.notFound.text", "The page you requested does not exist.")}</p>
                            </HtmlContentBlock>
                        </Notification>
                    </GridColumn>
                </div>
            </GridRow>
        </Grid>
    );
}
