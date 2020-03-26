import React, { useEffect, useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import { Button } from "@wrappers/index";
import Dialog from "@wrappers/blueprint/dialog";
import Loading from "../../Loading";
import { useDispatch, useSelector } from "react-redux";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";
import { SearchBar } from "../../SearchBar/SearchBar";
import Card from "@wrappers/blueprint/card";
import { ProjectForm } from "./ArtefactsForm/ProjectForm";
import { globalOp, globalSel } from "@ducks/global";

export interface IProps {

}

const ARTEFACT_FORM_COMPONENTS_MAP = {
    project: ProjectForm
};

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const modalStore = useSelector(globalSel.artefactModalSelector);
    const {selectedArtefact, isOpen, artefactsList} = modalStore;

    const [loading, setLoading] = useState<boolean>(false);
    const [selected, setSelected] = useState<string>(selectedArtefact);
    const [formData, setFormData] = useState<any>({});

    const handleAdd = () => {
        dispatch(globalOp.selectArtefact(selected))
    };

    const handleArtefactSelect = (artefactName: string) => {
        setSelected(artefactName)
    };

    const handleBack = () => {
        setSelected('');
        dispatch(globalOp.selectArtefact(''));
    };

    const handleCreate = () => {
        dispatch(globalOp.createArtefactAsync(formData));
        closeModal();
    };

    const closeModal = () => {
        dispatch(globalOp.closeArtefactModal());
        setFormData({});
    };

    const handleFormChange = (data: any) => {
        setFormData(data);
    };

    const ArtefactForm = ARTEFACT_FORM_COMPONENTS_MAP[modalStore.selectedArtefact];

    const buttonDisable = formData.label == '' || formData.label == undefined;

    return (
        <Dialog
            icon="info-sign"
            onClose={closeModal}
            title={`Create a new artefact${selectedArtefact && `: ${selectedArtefact}`}`}
            isOpen={isOpen}
            style={{width: '800px'}}
        >
            {
                loading ? <Loading/> : <>
                    <div className={Classes.DIALOG_BODY}>
                        {
                            ArtefactForm
                                ? <ArtefactForm onChange={handleFormChange}/>
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
                                                <Button onClick={() => handleArtefactSelect('project')}>Project</Button>
                                            </Col>
                                            {
                                                artefactsList.map(artefact =>
                                                    <Col key={artefact.key}>
                                                        <Button onClick={() => handleArtefactSelect(artefact.key)}>{artefact.title}</Button>
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
                                        <Button disabled={buttonDisable} affirmative={true} onClick={handleCreate}>Create</Button>
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
