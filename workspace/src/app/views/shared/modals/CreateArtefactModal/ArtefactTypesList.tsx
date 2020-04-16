import React from 'react';
import { TitleSubsection } from "@wrappers/index";
import { useSelector } from "react-redux";
import { globalSel } from "@ducks/global";

function ArtefactTypesList({ onSelect }) {
    const {selectedDType} = useSelector(globalSel.artefactModalSelector);
    const typeModifier = useSelector(globalSel.availableDTypesSelector).type;

    return <>
        <TitleSubsection>Artefact Type</TitleSubsection>
        <ul>
            <li
                key='all'
                onClick={() => onSelect('all')}
                className={selectedDType === 'all' ? 'active' : ''}
            >
                All
            </li>
            {
                typeModifier.options.map(type =>
                    <li
                        key={type.id}
                        onClick={() => onSelect(type.id)}
                        className={selectedDType === type.id ? 'active' : ''}
                    >{type.label}</li>
                )
            }
        </ul>
    </>
}

export default ArtefactTypesList;
