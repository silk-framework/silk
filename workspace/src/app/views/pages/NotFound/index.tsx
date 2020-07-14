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
} from "@wrappers/index";
import { PUBLIC_URL, SERVE_PATH } from "../../../constants/path";
import { useTranslation } from "react-i18next";

export default function () {
    const [t] = useTranslation();

    return (
        <Grid>
            <GridRow fullHeight>
                <div style={{ margin: "auto", maxWidth: "40rem" }}>
                    <GridColumn verticalAlign={"center"}>
                        <Notification
                            danger
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
                                    {t("pages.notFound.title", "Error 404: Content not found.")}
                                </TitleMainsection>
                                <p>
                                    {t(
                                        "pages.notFound.text",
                                        "If you think something went wrong then inform your administrator."
                                    )}
                                </p>
                            </HtmlContentBlock>
                        </Notification>
                    </GridColumn>
                </div>
            </GridRow>
        </Grid>
    );
}
