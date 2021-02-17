import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@gui-elements/index";
import Filterbar from "./Filterbar";
import SearchList from "../../shared/SearchList";
import SearchBar from "../../shared/SearchBar";
import { useTranslation } from "react-i18next";

const Artefacts = () => {
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const error = useSelector(workspaceSel.errorSelector);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
    };

    return (
        <WorkspaceContent className="eccapp-di__workspace">
            <WorkspaceMain>
                <Section>
                    <SectionHeader>
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>{t("pages.workspace.contents", "Contents")}</TitleMainsection>
                                </GridColumn>
                                <GridColumn full>
                                    <SearchBar
                                        focusOnCreation={true}
                                        textQuery={textQuery}
                                        sorters={sorters}
                                        onSort={handleSort}
                                        onSearch={handleSearch}
                                    />
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar />
                            </GridColumn>
                            <GridColumn full>
                                {error.detail ? (
                                    <Notification
                                        danger={true}
                                        actions={
                                            <Button
                                                text={t("common.action.retry", "Retry")}
                                                onClick={() => { window.location.reload(); }}
                                            />
                                        }
                                    >
                                        <h3>{t("http.error.fetchNotResult", "Error, cannot fetch results.")}</h3>
                                        <p>{error.detail}</p>
                                    </Notification>
                                ) : (
                                    <SearchList />
                                )}
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section></Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
};

export default Artefacts;
