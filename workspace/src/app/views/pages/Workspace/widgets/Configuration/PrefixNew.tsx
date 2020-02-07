import React, { useState } from 'react';
import Card from '@wrappers/card';
import Button from '@wrappers/button';
import InputGroup from '@wrappers/input-group';
import { Intent } from "@wrappers/constants";
import { IFormattedPrefix } from "./PrefixesDialog";

const PrefixNew = ({ onAdd }) => {
    const [newPrefix, setNewPrefix] = useState<IFormattedPrefix>({
        prefixName: '',
        prefixUri: ''
    });

    const handleChange = (field: string, value: string) => {
        setNewPrefix({
            ...newPrefix,
            [field]: value
        });
    };

    const handleAdd = () => {
        onAdd(newPrefix);
        setNewPrefix({
            prefixName: '',
            prefixUri: ''
        });
    };

    return (
        <Card>
            <h4>Add Prefix</h4>
            <div className='row'>
                <div className="col-5">
                    <InputGroup
                        value={newPrefix.prefixName}
                        onChange={(e) => handleChange('prefixName', e.target.value)}
                        placeholder={'Prefix Name'}/>
                </div>
                <div className="col-6">
                    <InputGroup
                        value={newPrefix.prefixUri}
                        onChange={(e) => handleChange('prefixUri', e.target.value)}
                        placeholder={'Prefix URI'}
                    />
                </div>
                <div className="col-1">
                    <Button intent={Intent.SUCCESS} onClick={handleAdd}>Add</Button>
                </div>
            </div>
        </Card>
    );
};

export default PrefixNew;
