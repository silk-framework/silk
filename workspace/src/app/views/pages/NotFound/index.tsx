import React from "react";
import {
    Grid,
    GridRow,
    GridColumn,
    Notification,
    TitleMainsection,
    HtmlContentBlock,
    Button,
    Icon,
} from "@eccenca/gui-elements";
import { PUBLIC_URL, SERVE_PATH } from "../../../constants/path";
import { useTranslation } from "react-i18next";

interface NotFoundProps {
    title?: string;
    message?: string;
}

export default function NotFound({ title, message }: NotFoundProps) {
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
                                <TitleMainsection>
                                    {title ?? t("pages.notFound.title", "Page not found.")}
                                </TitleMainsection>
                                <p>{message ?? t("pages.notFound.text", "The page you requested does not exist.")}</p>
                            </HtmlContentBlock>
                        </Notification>
                    </GridColumn>
                </div>
            </GridRow>
        </Grid>
    );
}
