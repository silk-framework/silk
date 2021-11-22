import React from "react";
import { FieldItem, AutoCompleteField, Notification, SimpleDialog, Button } from "@gui-elements/index";
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

    /** current project context */
    selectedProject: ISearchResultsServer | undefined;

    /**getWorkspace Projects*/
    getWorkspaceProjects: (textQuery: string) => Promise<ISearchResultsServer[]>;
}

const ProjectSelection: React.FC<ProjectSelectionProps> = ({
    setCurrentProject,
    onClose,
    shouldShowWarningModal,
    resetForm,
    selectedProject,
    getWorkspaceProjects,
}) => {
    const projectId = selectedProject?.id;
    const [showModal, setShowModal] = React.useState<boolean>(false);
    const [newProject, setNewProject] = React.useState<ISearchResultsServer | null>();

    /**
     * Warning prompt that shows when there are task form changes other label/description
     */
    const warningModalForChangingProject = (
        <SimpleDialog
            size="small"
            isOpen={true}
            title="Project change warning"
            actions={[
                <Button
                    text="Confirm"
                    hasStateDanger
                    onClick={() => {
                        resetForm();
                        setCurrentProject(newProject!);
                    }}
                />,
                <Button text="Cancel" onClick={onClose} />,
            ]}
        >
            {/** //Todo add translation here **/}
            <p>All settings except title/description are going to be reset</p>
        </SimpleDialog>
    );

    return (
        <>
            {showModal && newProject ? warningModalForChangingProject : null}
            <FieldItem
                key={"copy-label"}
                // TODO ADD translation
                labelAttributes={{
                    htmlFor: "project-select",
                    text: "Select project",
                }}
            >
                <AutoCompleteField
                    autoFocus={!!selectedProject}
                    onSearch={getWorkspaceProjects}
                    onChange={(item) => {
                        if (item) {
                            const show = shouldShowWarningModal(item.id !== projectId);
                            setNewProject(item);
                            setShowModal(show);
                            if (!projectId || !show) {
                                resetForm();
                                setCurrentProject(item);
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
                    // TODO ADD translation
                    noResultText={"No Result"}
                />
            </FieldItem>
            {/* // TODO ADD translation */}
            {(!projectId && <Notification message="Please select project first, before configuration." />) || null}
        </>
    );
};

export default ProjectSelection;
