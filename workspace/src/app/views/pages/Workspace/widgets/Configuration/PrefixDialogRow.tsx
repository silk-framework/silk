import { IconNames } from "@wrappers/constants";
import React, { useEffect, useState } from "react";
import InputGroup from "@wrappers/input-group";
import Icon from "@wrappers/icon";
import { IFormattedPrefix } from "./PrefixesDialog";

interface IProps {
    prefix: IFormattedPrefix;
    onChange(field: string, value: string);
    onRemove();
}

const PrefixDialogRow = ({prefix, onChange, onRemove}: IProps) => {
    const [updatedPrefix, setUpdatedPrefix] = useState<IFormattedPrefix>(prefix);

    useEffect(() => {
        setUpdatedPrefix(prefix);
    }, [prefix]);

    const handleChange = (field: string, value: string) => {
        setUpdatedPrefix({
            ...updatedPrefix,
            [field]: value
        });
    };

    return (
        <div className='row'>
            <div className="col-5">
                <InputGroup
                    placeholder='Prefix Name'
                    value={updatedPrefix.prefixName}
                    onChange={(e) => handleChange('prefixName', e.target.value)}/>
            </div>
            <div className="col-6">
                <InputGroup
                    placeholder='Prefix Uri'
                    value={updatedPrefix.prefixUri}
                    onChange={(e) => handleChange('prefixUri', e.target.value)}/>
            </div>
            <div className="col-1">
                <Icon
                    icon={IconNames.TRASH}
                    onClick={onRemove}
                />
            </div>
        </div>
    )
};

export default PrefixDialogRow;
