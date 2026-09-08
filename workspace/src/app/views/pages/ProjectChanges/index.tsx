import React from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import {
    Divider,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@eccenca/gui-elements";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { requestProjectMetadata } from "@ducks/shared/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { SERVE_PATH } from "../../../constants/path";
import NotFound from "../NotFound";
import ChangeList from "./ChangeList";

/** The change journal of a project: what has been changed, and reverting single changes. */
const ProjectChanges = () => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const { projectId } = useParams<{ projectId: string }>();
    const [notFound, setNotFound] = React.useState(false);
    const [refreshKey, setRefreshKey] = React.useState(0);

    const breadcrumbs = [
        {
            text: t("navigation.side.diBrowse"),
            href: SERVE_PATH,
        },
        {
            text: t("pages.changes.title"),
            current: true,
        },
    ];

    const { pageHeader, updatePageHeader } = usePageHeader({
        alternateDepiction: "artefact-commit",
        autogeneratePageTitle: true,
        breadcrumbs,
    });

    React.useEffect(() => {
        requestProjectMetadata(projectId)
            .then((res) => {
                updatePageHeader({
                    breadcrumbs: [
                        breadcrumbs[0],
                        {
                            text: res.data.label,
                            href: `${SERVE_PATH}/projects/${projectId}`,
                        },
                        breadcrumbs[1],
                    ],
                    pageTitle: `${breadcrumbs[1].text}: ${res.data.label}`,
                });
            })
            .catch((ex) => {
                if (ex?.httpStatus === 404) {
                    setNotFound(true);
                } else {
                    registerError("ProjectChanges.projectMetadata", t("pages.changes.errors.fetchProject"), ex);
                }
            });
    }, [projectId]);

    if (notFound) {
        return <NotFound />;
    }

    return (
        <WorkspaceContent>
            {pageHeader}
            <WorkspaceMain>
                <Section>
                    <SectionHeader>
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>{t("pages.changes.title", "Changes")}</TitleMainsection>
                                </GridColumn>
                                <GridColumn>
                                    <div style={{ display: "flex", justifyContent: "flex-end" }}>
                                        <IconButton
                                            data-test-id={"changes-reload-btn"}
                                            name="item-reload"
                                            text={t("pages.changes.reload")}
                                            onClick={() => setRefreshKey((key) => key + 1)}
                                        />
                                    </div>
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn>
                                <ChangeList projectId={projectId} refreshKey={refreshKey} />
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide></WorkspaceSide>
        </WorkspaceContent>
    );
};

export default ProjectChanges;
