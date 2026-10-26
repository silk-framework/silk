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
import { absoluteProjectPath, getFullRoutePath } from "../../../utils/routerUtils";
import NotFound from "../NotFound";
import MarkdownModal from "../../shared/modals/MarkdownModal";
import ChangeList from "./ChangeList";
import { changeJournalHelpMarkdown } from "./changeJournalHelp";

/** The change journal of a project: what has been changed, and reverting single changes. */
const ProjectChangesPage = ({ projectId }: { projectId: string }) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [notFound, setNotFound] = React.useState(false);
    const [refreshKey, setRefreshKey] = React.useState(0);
    const [helpOpen, setHelpOpen] = React.useState(false);

    const breadcrumbs = [
        {
            text: t("common.app.build", "Workbench"),
            href: getFullRoutePath("?itemType=project"),
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
                            href: absoluteProjectPath(projectId),
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
                                        <IconButton
                                            data-test-id={"changes-help-btn"}
                                            name="item-question"
                                            text={t("pages.changes.help.open")}
                                            onClick={() => setHelpOpen(true)}
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
            <MarkdownModal
                data-test-id={"changes-help-modal"}
                isOpen={helpOpen}
                onDiscard={() => setHelpOpen(false)}
                title={t("pages.changes.help.title")}
                markdown={changeJournalHelpMarkdown}
                size="large"
            />
        </WorkspaceContent>
    );
};

/**
 * The changes page of the project in the URL. The router keeps the page when only the project changes, so the page
 * is keyed by the project: nothing of the previous project is kept, and a late answer for it is not shown.
 */
const ProjectChanges = () => {
    const { projectId } = useParams<{ projectId: string }>();
    return <ProjectChangesPage key={projectId} projectId={projectId} />;
};

export default ProjectChanges;
