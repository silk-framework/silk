import React from 'react';
import InputGroup from '@wrappers/blueprint/input-group';
import {
    Button,
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    WorkspaceGrid,
    WorkspaceRow,
    WorkspaceColumn,
} from '@wrappers/index';

const PrefixNew = ({onAdd, onChangePrefix, prefix}) => {
    return (
        <Card elevated={0}>
            <CardHeader>
                <CardTitle>
                    <h4>Add Prefix</h4>
                </CardTitle>
            </CardHeader>
            <CardContent>
                <WorkspaceGrid>
                    <WorkspaceRow>
                        <WorkspaceColumn medium>
                            <InputGroup
                                value={prefix.prefixName}
                                onChange={(e) => onChangePrefix('prefixName', e.target.value)}
                                placeholder={'Prefix Name'}/>
                        </WorkspaceColumn>
                        <WorkspaceColumn>
                            <InputGroup
                                value={prefix.prefixUri}
                                onChange={(e) => onChangePrefix('prefixUri', e.target.value)}
                                placeholder={'Prefix URI'}
                            />
                        </WorkspaceColumn>
                        <WorkspaceColumn small>
                            <Button
                                onClick={onAdd}
                                elevated
                                disabled={!prefix.prefixName || !prefix.prefixUri}
                            >
                                Add
                            </Button>
                        </WorkspaceColumn>
                    </WorkspaceRow>
                </WorkspaceGrid>
            </CardContent>
        </Card>
    );
};

export default PrefixNew;
