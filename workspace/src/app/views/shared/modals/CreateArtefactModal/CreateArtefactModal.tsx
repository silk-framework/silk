import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import {
    Button,
    Grid,
    GridRow,
    GridColumn,
    Spacing,
    TitleSubsection,
    SimpleDialog,
    Menu,
    MenuItem,
} from "@wrappers/index";
import { globalOp, globalSel } from "@ducks/global";
import { IArtefactItem } from "@ducks/global/typings";
import Loading from "../../Loading";
import { SearchBar } from "../../SearchBar/SearchBar";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";

const ARTEFACT_FORM_COMPONENTS_MAP = {
    project: ProjectForm
};

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const modalStore = useSelector(globalSel.artefactModalSelector);
    const projectId = useSelector(globalSel.currentProjectIdSelector);

    const {selectedArtefact, isOpen, artefactsList} = modalStore;

    const [loading, setLoading] = useState<boolean>(false);
    const [selected, setSelected] = useState<IArtefactItem>(selectedArtefact);

    const handleAdd = () => {
        dispatch(globalOp.selectArtefact(selected))
    };

    const handleArtefactSelect = (artefact: IArtefactItem) => {
        setSelected(artefact)
    };

    const handleBack = () => {
        setSelected(null);
        dispatch(globalOp.selectArtefact(null));
    };

    const handleCreate = (e) => {
        e.preventDefault();

        const isValidFields = form.triggerValidation();
        if (isValidFields) {
            dispatch(globalOp.createArtefactAsync(form.getValues()));
            closeModal();
        }
    };

    const closeModal = () => {
        dispatch(globalOp.closeArtefactModal());
        form.clearError();
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const _TEMP_handleProjectSelect = () => {
        handleArtefactSelect({
            key: 'project'
        } as IArtefactItem);
    };

    let artefactForm = null;
    if (modalStore.selectedArtefact) {
        const {key} = modalStore.selectedArtefact;
        const ComponentForm = ARTEFACT_FORM_COMPONENTS_MAP[key];

        artefactForm = projectId && !ComponentForm
            ? <TaskForm form={form} artefact={selected} projectId={projectId}/>
            : <ComponentForm form={form}/>
    }

    return (
        <SimpleDialog
            size="large"
            hasBorder
            title={`Create a new artefact${selectedArtefact ? `: ${selectedArtefact.title}` : ''}`}
            onClose={closeModal}
            isOpen={isOpen}
            actions={
                selectedArtefact ? [
                    <Button
                        key='create'
                        affirmative={true}
                        onClick={handleCreate}
                        disabled={isErrorPresented()}
                    >
                        Create
                    </Button>,
                    <Button key='cancel' onClick={closeModal}>Cancel</Button>,
                    <Button key='back' onClick={handleBack}>Back</Button>
                ] : [
                    <Button
                        key='add'
                        affirmative={true}
                        onClick={handleAdd}
                        disabled={!selected}
                    >
                        Add
                    </Button>,
                    <Button key='cancel' onClick={closeModal}>Cancel</Button>
                ]
            }
        >
            {
                loading ?
                    <Loading/> : <>
                        {
                            artefactForm ?
                                artefactForm : (
                                    <Grid>
                                        <GridRow>
                                            <GridColumn small>
                                                <TitleSubsection>Artefact Type</TitleSubsection>
                                                <Menu>
                                                    <MenuItem text={'All'} active />
                                                </Menu>
                                            </GridColumn>
                                            <GridColumn>
                                                <SearchBar onSort={() => {
                                                }} onApplyFilters={() => {
                                                }}/>
                                                <Spacing/>
                                                <Grid>
                                                    <GridRow>
                                                        <GridColumn>
                                                            <Button onClick={_TEMP_handleProjectSelect}>
                                                                Project
                                                            </Button>
                                                        </GridColumn>
                                                        {
                                                            projectId && artefactsList.map(artefact =>
                                                                <GridColumn key={artefact.key}>
                                                                    <Button
                                                                        onClick={() => handleArtefactSelect(artefact)}>{artefact.title}</Button>
                                                                </GridColumn>
                                                            )
                                                        }
                                                    </GridRow>
                                                </Grid>
                                            </GridColumn>
                                        </GridRow>
                                    </Grid>
                                )
                        }
                    </>
            }
        </SimpleDialog>
    )
}
