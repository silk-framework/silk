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
} from "@wrappers/index";
import { globalOp, globalSel } from "@ducks/common";
import { IArtefactItem, IDetailedArtefactItem } from "@ducks/common/typings";
import Loading from "../../Loading";
import { SearchBar } from "../../SearchBar/SearchBar";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import ArtefactTypesList from "./ArtefactTypesList";

const PROJECT_KEY = 'project';

type TSelectedArtefact = IArtefactItem | typeof PROJECT_KEY;

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const modalStore = useSelector(globalSel.artefactModalSelector);
    const projectId = useSelector(globalSel.currentProjectIdSelector);

    const {selectedArtefact, isOpen, artefactsList, cachedArtefactProperties, loading} = modalStore;

    // initially take from redux
    const [selected, setSelected] = useState<TSelectedArtefact>(selectedArtefact);

    const handleAdd = () => {
        if (selected === PROJECT_KEY) {
            return dispatch(globalOp.selectArtefact({
                key: PROJECT_KEY
            }));
        }
        dispatch(globalOp.getArtefactPropertiesAsync(selected as IArtefactItem))
    };

    const handleArtefactSelect = (artefact: TSelectedArtefact) => {
        setSelected(artefact);
    };

    const handleBack = () => {
        resetModal();
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
        resetModal();
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const handleSelectDType = (value: string) => {
        dispatch(globalOp.setSelectedArtefactDType(value));
    };

    const resetModal = () => {
        setSelected(null);
        form.clearError();
    };

    let artefactForm = null;


    if (modalStore.selectedArtefact) {
        const {key} = modalStore.selectedArtefact;
        if (key === PROJECT_KEY) {
            artefactForm = <ProjectForm form={form}/>
        } else {
            const detailedArtefact = cachedArtefactProperties[key];
            artefactForm = projectId
                ? <TaskForm form={form} artefact={detailedArtefact} projectId={projectId}/>
                : null;
        }

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
                                                <ArtefactTypesList onSelect={handleSelectDType}/>
                                            </GridColumn>
                                            <GridColumn>
                                                <SearchBar onSort={() => {}} onApplyFilters={() => {}}/>
                                                <Spacing/>
                                                <Grid>
                                                    <GridRow>
                                                        <GridColumn>
                                                            <Button onClick={() => handleArtefactSelect(PROJECT_KEY)}>
                                                                Project
                                                            </Button>
                                                        </GridColumn>
                                                        {
                                                            projectId && artefactsList.map(artefact =>
                                                                <GridColumn key={artefact.key}>
                                                                    <Button onClick={() => handleArtefactSelect(artefact)}>{artefact.title}</Button>
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
