import React, { useEffect, useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import { Button } from "@wrappers/index";
import Dialog from "@wrappers/blueprint/dialog";
import Loading from "../../Loading";
import { useDispatch, useSelector } from "react-redux";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";
import { SearchBar } from "../../SearchBar/SearchBar";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { globalOp, globalSel } from "@ducks/global";
import { TaskForm } from "./ArtefactForms/TaskForm";
import { IArtefactItem } from "@ducks/global/typings";
import { useForm } from "react-hook-form";

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
    };

    const _TEMP_handleProjectSelect = () => {
        handleArtefactSelect({
            key: 'project'
        } as IArtefactItem);
    };

    let artefactForm = null;
    if (modalStore.selectedArtefact) {
        const {key} = modalStore.selectedArtefact;
        const ComponentForm = ARTEFACT_FORM_COMPONENTS_MAP[key];

        artefactForm = projectId
            ? <TaskForm form={form} artefact={selected} projectId={projectId} />
            : <ComponentForm form={form} />
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
                                : <Row>
                                    <Col span={4}>
                                        <h3>Artefact Type</h3>
                                        <ul>
                                            <li><a href='#'>All</a></li>
                                        </ul>
                                    </Col>
                                    <Col span={12}>
                                        <Row>
                                            <SearchBar onSort={() => {}} onApplyFilters={() => {}}/>
                                        </Row>
                                        <Row>
                                            <Col>
                                                <Button onClick={_TEMP_handleProjectSelect}>
                                                    Project
                                                </Button>
                                            </Col>
                                            {
                                                projectId && artefactsList.map(artefact =>
                                                    <Col key={artefact.key}>
                                                        <Button onClick={() => handleArtefactSelect(artefact)}>{artefact.title}</Button>
                                                    </Col>
                                                )
                                            }
                                        </Row>
                                    </Col>
                                </Row>
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
