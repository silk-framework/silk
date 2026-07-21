import React from "react";
import { FieldItem, SuggestField, Notification, Button, AlertDialog } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { ProjectIdAndLabel } from "../CreateArtefactModal";
import useHotKey from "../../../HotKeyHandler/HotKeyHandler";

interface ProjectSelectionProps {
    /** handle project selection **/
    setCurrentProject: (selectedProject: ISearchResultsServer) => void;

    /** Decide whether to show modal or not by factoring both the form changes and whether or not a project has been selected **/
    modifiedValuesExist: () => boolean;

    /** reset the form if there have been entries other than label/description **/
    resetForm: () => void;

    /** current project context */
    selectedProject: ProjectIdAndLabel | undefined;

    /**getWorkspace Projects*/
    getWorkspaceProjects: (textQuery: string) => Promise<ISearchResultsServer[]>;
}

const ProjectSelection: React.FC<ProjectSelectionProps> = ({
    setCurrentProject,
    modifiedValuesExist,
    resetForm,
    selectedProject,
    getWorkspaceProjects,
}) => {
    const projectId = selectedProject?.id;
    const [t] = useTranslation();
    const [showWarningModal, setShowWarningModal] = React.useState<boolean>(false);
    const [newProject, setNewProject] = React.useState<ISearchResultsServer | null>();
    // Bumped when a pending project change is cancelled, to remount the suggest field with the still-current project.
    const [discardedSelections, setDiscardedSelections] = React.useState<number>(0);

    /**
     * Warning prompt that shows when there are task form changes other label/description
     */
    const WarningModalForChangingProject = () => {
        const onSubmit = () => {
            resetForm();
            setCurrentProject(newProject!);
            setShowWarningModal(false);
            setNewProject(null);
        };
        const onCancel = () => {
            setShowWarningModal(false);
            setNewProject(null);
            setDiscardedSelections((count) => count + 1);
        };
        useHotKey({ hotkey: "enter", handler: onSubmit });
        return (
            <AlertDialog
                danger
                size="tiny"
                isOpen={true}
                canEscapeKeyClose={true}
                onClose={onCancel}
                title={t("CreateModal.projectContext.resetModalTitle", "Project change warning")}
                actions={[
                    <Button text={t("CreateModal.projectContext.changeProjectButton", "Ok")} onClick={onSubmit} />,
                    <Button text={t("common.action.cancel", "Cancel")} onClick={onCancel} />,
                ]}
            >
                <p>
                    {t(
                        "CreateModal.projectContext.configResetInfo",
                        "All settings except title/description are going to be reset.",
                    )}
                </p>
            </AlertDialog>
        );
    };

    return (
        <>
            {showWarningModal && newProject ? <WarningModalForChangingProject /> : null}
            <FieldItem
                key={"copy-label"}
                labelProps={{
                    htmlFor: "project-select",
                    text: t("CreateModal.projectContext.selectProjectLabel", "Select project"),
                }}
            >
                <SuggestField<ISearchResultsServer, ISearchResultsServer | null>
                    key={`${projectId ?? ""}-${discardedSelections}`}
                    initialValue={
                        selectedProject
                            ? { id: selectedProject.id, label: selectedProject.label, type: "project" }
                            : undefined
                    }
                    onSearch={getWorkspaceProjects}
                    onChange={(item) => {
                        if (item) {
                            const show = item.id !== projectId && modifiedValuesExist();
                            setNewProject(item);
                            setShowWarningModal(show);
                            if (!show) {
                                resetForm();
                                setCurrentProject(item);
                            }
                        }
                    }}
                    itemValueRenderer={(item) => item.label}
                    itemValueSelector={(item: ISearchResultsServer) => item}
                    itemRenderer={(item) => item.label}
                    itemValueString={(item) => item.id}
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
                        "Please select project first, before configuration.",
                    )}
                />
            )) ||
                null}
        </>
    );
};

export default ProjectSelection;
