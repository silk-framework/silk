import { IconNames } from "@wrappers/constants";
import React from "react";
import InputGroup from "@wrappers/input-group";
import Icon from "@wrappers/icon";

interface IProps {
    itemKey: string;
    value: string;
    onChange(value: string);
    onRemove();
}

const PrefixDialogRow = ({itemKey, value, onChange, onRemove}: IProps) => {
    return (
        <div className='row' key={itemKey}>
            <div className="col-5">
                <InputGroup value={itemKey} onChange={(e) => onChange(e.target.value)}/>
            </div>
            <div className="col-6">
                <InputGroup value={value} onChange={(e) => onChange(e.target.value)}/>
            </div>
            <div className="col-1">
                <Icon icon={IconNames.TRASH} onClick={onRemove}/>
            </div>
        </div>
    )
};

export default PrefixDialogRow;
