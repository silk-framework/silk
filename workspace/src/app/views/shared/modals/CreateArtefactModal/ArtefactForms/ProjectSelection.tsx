import React from "react";
import { FieldItem, AutoCompleteField, Notification, AlertDialog, Button } from "@gui-elements/index";
import { useSelector, useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import { commonOp, commonSel } from "@ducks/common";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { requestSearchList } from "@ducks/workspace/requests";

interface ProjectSelectionProps {
    /** handle project selection **/
    setCurrentProject: (selectedProject: ISearchResultsServer) => void;

    /** revert back to info Notification **/
    onClose: () => void;

    /** Decide whether to show modal or not by factoring both the form changes and whether or not a project has been selected **/
    shouldShowWarningModal: (precondition: boolean) => boolean;

    /** reset the form if there have been entries other than label/description **/
    resetForm: () => void;
}

const ProjectSelection: React.FC<ProjectSelectionProps> = ({
    setCurrentProject,
    onClose,
    shouldShowWarningModal,
    resetForm,
}) => {
    const dispatch = useDispatch();
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();
    const [showModal, setShowModal] = React.useState<boolean>(false);
    const [newProject, setNewProject] = React.useState<ISearchResultsServer | null>();

    const handleSearch = async (textQuery: string): Promise<ISearchResultsServer[]> => {
        try {
            const payload = {
                limit: 10,
                offset: 0,
                itemType: "project",
                textQuery,
            };
            const results = (await requestSearchList(payload)).results;
            return results;
        } catch (err) {
            console.warn("err", { err });
            return [];
        }
    };

    /**
     * sets selected projectId to global state.
     * sets selected project as context for artefact modal
     * @param project
     */
    const handleProjectUpdate = (project: ISearchResultsServer) => {
        dispatch(commonOp.setProjectId(project.id));
        setCurrentProject(project);
    };

    /**
     * Warning prompt that shows when there are task form changes other label/description
     */
    const warningModalForChangingProject = (
        <AlertDialog
            danger
            size="tiny"
            isOpen={true}
            title={t("CreateModal.projectContext.resetModalTitle", "Project change warning")}
            actions={[
                <Button
                    text={t("CreateModal.projectContext.changeProjectButton", "Ok")}
                    onClick={() => {
                        resetForm();
                        handleProjectUpdate(newProject!);
                    }}
                />,
                <Button text={t("common.action.cancel", "Cancel")} onClick={onClose} />,
            ]}
        >
            <p>
                {t(
                    "CreateModal.projectContext.configResetInfo",
                    "All settings except title/description are going to be reset."
                )}
            </p>
        </AlertDialog>
    );

    return (
        <>
            {showModal && newProject ? warningModalForChangingProject : null}
            <FieldItem
                key={"copy-label"}
                labelAttributes={{
                    htmlFor: "project-select",
                    text: t("CreateModal.projectContext.selectProjectLabel", "Select project"),
                }}
            >
                <AutoCompleteField
                    autoFocus
                    onSearch={handleSearch}
                    onChange={(item) => {
                        if (item) {
                            const show = shouldShowWarningModal(item.id !== projectId);
                            setNewProject(item);
                            setShowModal(show);
                            if (!projectId || !show) {
                                handleProjectUpdate(item);
                            }
                        }
                    }}
                    popoverProps={{
                        onClosed: () => {
                            projectId && !showModal && onClose();
                        },
                    }}
                    itemValueRenderer={(item) => item.label}
                    itemValueSelector={(item: ISearchResultsServer) => item}
                    itemRenderer={(item) => item.label}
                    reset={{
                        resettableValue: () => true,
                        resetValue: null,
                        resetButtonText: "operation-clear",
                    }}
                    noResultText={t("CreateModal.projectContext.noOptions", "No Result")}
                />
            </FieldItem>
            {(!projectId && (
                <Notification
                    message={t(
                        "CreateModal.projectContext.selectProjectInfo",
                        "Please select project first, before configuration."
                    )}
                />
            )) ||
                null}
        </>
    );
};

export default ProjectSelection;
