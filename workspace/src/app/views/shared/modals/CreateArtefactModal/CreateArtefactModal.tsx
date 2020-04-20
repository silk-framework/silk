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
    OverviewItemList,
    OverviewItem,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Card,
    Icon,
    HelperClasses,
} from "@wrappers/index";
import { globalOp, globalSel } from "@ducks/common";
import { IArtefactItem, IDetailedArtefactItem } from "@ducks/common/typings";
import Loading from "../../Loading";
import { SearchBar } from "../../SearchBar/SearchBar";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import ArtefactTypesList from "./ArtefactTypesList";
import { DATA_TYPES } from "../../../../constants";

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const modalStore = useSelector(globalSel.artefactModalSelector);
    const projectId = useSelector(globalSel.currentProjectIdSelector);

    const {selectedArtefact, isOpen, artefactsList, cachedArtefactProperties, loading} = modalStore;

    // initially take from redux
    const [selected, setSelected] = useState<IArtefactItem>(selectedArtefact);

    const handleAdd = () => {
        if (selected.key === DATA_TYPES.PROJECT) {
            return dispatch(globalOp.selectArtefact(selected));
        }
        dispatch(globalOp.getArtefactPropertiesAsync(selected))
    };

    const handleArtefactSelect = (artefact: IArtefactItem) => {
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
        setSelected({} as IArtefactItem);
        form.clearError();
    };

    let artefactForm = null;
    if (selectedArtefact.key) {
        if (selectedArtefact.key === DATA_TYPES.PROJECT) {
            artefactForm = <ProjectForm form={form}/>
        } else {
            const detailedArtefact = cachedArtefactProperties[selectedArtefact.key];
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
                selectedArtefact.key ? [
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
                                                <OverviewItemList hasSpacing columns={2}>
                                                    <Card
                                                        isOnlyLayout
                                                        className={
                                                            (selected.key === DATA_TYPES.PROJECT) ? HelperClasses.Intent.ACCENT : ''
                                                        }
                                                    >
                                                        <OverviewItem
                                                            hasSpacing
                                                            onClick={() => handleArtefactSelect({key: DATA_TYPES.PROJECT})}
                                                        >
                                                            <OverviewItemDepiction>
                                                                <Icon name='artefact-project' large />
                                                            </OverviewItemDepiction>
                                                            <OverviewItemDescription>
                                                                <OverviewItemLine>
                                                                    <strong>Project</strong>
                                                                </OverviewItemLine>
                                                                <OverviewItemLine small>
                                                                    <p>Lorem Ipsum</p>
                                                                </OverviewItemLine>
                                                            </OverviewItemDescription>
                                                        </OverviewItem>
                                                    </Card>
                                                        {
                                                            projectId && artefactsList.map(artefact =>
                                                                <Card
                                                                    isOnlyLayout
                                                                    key={artefact.key}
                                                                    className={
                                                                        (selected.key === artefact.key) ? HelperClasses.Intent.ACCENT : ''
                                                                    }
                                                                >
                                                                    <OverviewItem
                                                                        hasSpacing
                                                                        onClick={() => handleArtefactSelect(artefact)}
                                                                    >
                                                                        <OverviewItemDepiction>
                                                                            <Icon name={'artefact-' + artefact.key} large />
                                                                        </OverviewItemDepiction>
                                                                        <OverviewItemDescription>
                                                                            <OverviewItemLine>
                                                                                <strong>{artefact.title}</strong>
                                                                            </OverviewItemLine>
                                                                            <OverviewItemLine small>
                                                                                <p>{artefact.description}</p>
                                                                            </OverviewItemLine>
                                                                        </OverviewItemDescription>
                                                                    </OverviewItem>
                                                                </Card>
                                                            )
                                                        }
                                                </OverviewItemList>
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
