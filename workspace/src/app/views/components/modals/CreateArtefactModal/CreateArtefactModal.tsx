import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";
import { globalOp, globalSel } from "@ducks/global";
import { IArtefactItem } from "@ducks/global/typings";
import {
    Button,
    WorkspaceGrid,
    WorkspaceRow,
    WorkspaceColumn,
    Spacing,
    TitleSubsection,
} from "@wrappers/index";
import { GenericForm } from "./ArtefactForms/GenericForm";
import Loading from "../../Loading";
import { SearchBar } from "../../SearchBar/SearchBar";
import { ProjectForm } from "./ArtefactForms/ProjectForm";

const ARTEFACT_FORM_COMPONENTS_MAP = {
    project: ProjectForm
};

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm({
        mode: 'onChange'
    });

    const modalStore = useSelector(globalSel.artefactModalSelector);
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
    };

    const _TEMP_handleProjectSelect = () => {
        handleArtefactSelect({
            key: 'project'
        } as IArtefactItem);
    };

    let artefactForm = null;
    if (modalStore.selectedArtefact) {
        const {key, properties, required} = modalStore.selectedArtefact;
        const ComponentForm = ARTEFACT_FORM_COMPONENTS_MAP[key];

        artefactForm = ComponentForm
            ? <ComponentForm form={form} />
            : <GenericForm form={form} properties={properties} required={required}/>
    }

    return (
        <Dialog
            icon="info-sign"
            onClose={closeModal}
            title={`Create a new artefact${selectedArtefact ? `: ${selectedArtefact.title}` : ''}`}
            isOpen={isOpen}
            style={{width: '800px'}}
        >
            {
                loading ? <Loading/> : <>
                    <div className={Classes.DIALOG_BODY}>
                        {
                            artefactForm
                                ? artefactForm
                                : (
                                    <WorkspaceGrid>
                                        <WorkspaceRow>
                                           <WorkspaceColumn small>
                                               <TitleSubsection>Artefact Type</TitleSubsection>
                                               <ul>
                                                   <li><a href='#'>All</a></li>
                                               </ul>
                                           </WorkspaceColumn>
                                           <WorkspaceColumn>
                                                <SearchBar onSort={() => {}} onApplyFilters={() => {}}/>
                                                <Spacing />
                                                <WorkspaceGrid>
                                                    <WorkspaceRow>
                                                        <WorkspaceColumn>
                                                            <Button onClick={_TEMP_handleProjectSelect}>
                                                                Project
                                                            </Button>
                                                        </WorkspaceColumn>
                                                       {
                                                           artefactsList.map(artefact =>
                                                               <WorkspaceColumn key={artefact.key}>
                                                                   <Button onClick={() => handleArtefactSelect(artefact)}>{artefact.title}</Button>
                                                               </WorkspaceColumn>
                                                           )
                                                       }
                                                    </WorkspaceRow>
                                                </WorkspaceGrid>
                                           </WorkspaceColumn>
                                        </WorkspaceRow>
                                    </WorkspaceGrid>
                                )
                        }
                    </div>
                    <div className={Classes.DIALOG_FOOTER}>
                        <div className={Classes.DIALOG_FOOTER_ACTIONS}>

                            {
                                selectedArtefact
                                    ? <>
                                        <Button onClick={handleBack}>Back</Button>
                                        <Button affirmative={true} onClick={handleCreate}>Create</Button>
                                    </>
                                    : <Button
                                        affirmative={true}
                                        onClick={handleAdd}
                                        disabled={!selected}>
                                        Add
                                    </Button>
                            }

                            <Button onClick={closeModal}>Cancel</Button>
                        </div>
                    </div>
                </>
            }
        </Dialog>
    )
}
