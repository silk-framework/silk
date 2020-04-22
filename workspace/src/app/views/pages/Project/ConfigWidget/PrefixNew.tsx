import React from 'react';
import {
    Button,
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Grid,
    GridRow,
    GridColumn,
    TextField,
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
                <Grid>
                    <GridRow>
                        <GridColumn medium>
                            <TextField
                                value={prefix.prefixName}
                                onChange={(e) => onChangePrefix('prefixName', e.target.value)}
                                placeholder={'Prefix Name'}/>
                        </GridColumn>
                        <GridColumn>
                            <TextField
                                value={prefix.prefixUri}
                                onChange={(e) => onChangePrefix('prefixUri', e.target.value)}
                                placeholder={'Prefix URI'}
                            />
                        </GridColumn>
                        <GridColumn small>
                            <Button
                                onClick={onAdd}
                                elevated
                                disabled={!prefix.prefixName || !prefix.prefixUri}
                            >
                                Add
                            </Button>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </CardContent>
        </Card>
    );
};

export default PrefixNew;
