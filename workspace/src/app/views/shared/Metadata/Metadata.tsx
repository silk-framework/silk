import React, { useEffect, useState } from 'react';
import { sharedOp } from "@ducks/shared";
import {
    Button,
    Card,
    CardActions,
    CardActionsAux,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    ContextMenu,
    Divider,
    IconButton,
    MenuItem,
} from "@wrappers/index";
import { IMetadata } from "@ducks/shared/thunks/metadata.thunk";
import { Loading } from "../Loading/Loading";

export function Metadata({projectId = null, taskId}) {
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState({} as IMetadata);

    useEffect(() => {
        getTaskMetadata(taskId, projectId);
    }, [taskId, projectId]);

    const getTaskMetadata = async (taskId: string, projectId: string) => {
        setLoading(true);
        const data = await sharedOp.getTaskMetadataAsync(taskId, projectId);
        setData(data);
        setLoading(false);
    };

    const {label, description} = data;

    return (
        <>
            <div className='metadata-block'>
                <Card>
                    <CardHeader>
                        <CardTitle>
                            <h4>Details & Metadata</h4>
                        </CardTitle>
                        <CardOptions>
                            <IconButton name="item-edit" text="Edit"/>
                            <ContextMenu>
                                <MenuItem text={'This'} disabled/>
                                <MenuItem text={'Is just a'} disabled/>
                                <MenuItem text={'Dummy'} disabled/>
                            </ContextMenu>
                        </CardOptions> : null
                    </CardHeader>
                    <Divider/>
                    <CardContent>
                        {loading ? <Loading/> : <div>
                            <p>Name: {label}</p>
                            {!!description && <p>Description: {description}</p>}
                        </div>}
                    </CardContent>
                    {!loading && <>
                        <Divider/>
                        <CardActions>
                            <Button text="Remove me" disruptive/>
                            <Button text="Dummy"/>
                            <CardActionsAux>
                                <Button text="Auxiliary action" minimal />
                            </CardActionsAux>
                        </CardActions>
                    </>}
                </Card>
            </div>
        </>
    );
}
