import React, { useEffect, useState } from 'react';
import { sharedOp } from "@ducks/shared";
import {
    Card,
    CardHeader,
    CardTitle,
    CardOptions,
    CardContent,
    CardActions,
    IconButton,
    ContextMenu,
    MenuItem,
    Button,
    Divider,
} from "@wrappers/index";

export default function ({ projectId = null, taskId }) {
    const [metadata, setMetadata] = useState({} as any);

    useEffect(() => {
        getTaskMetadata(taskId, projectId);
    }, [taskId, projectId]);

    const getTaskMetadata = async (taskId: string, projectId: string) => {
        const data = await sharedOp.getTaskMetadataAsync(taskId, projectId);
        setMetadata(data);
    };

    const { name, description, id } = metadata;
    return (
        <>
            <div className='metadata-block'>
                <Card>
                    <CardHeader>
                        <CardTitle>
                            <h4>Details & Metadata</h4>
                        </CardTitle>
                        <CardOptions>
                            <IconButton name="item-edit" text="Edit" />
                            <ContextMenu>
                                <MenuItem text={'This'} disabled />
                                <MenuItem text={'Is just a'} disabled />
                                <MenuItem text={'Dummy'} disabled />
                            </ContextMenu>
                        </CardOptions>
                    </CardHeader>
                    <Divider />
                    <CardContent>
                        <p>
                            Name: {name || id}
                        </p>
                        {
                            description && <p>Description: {description}</p>
                        }
                    </CardContent>
                    <Divider />
                    <CardActions inverseDirection>
                        <Button text="Remove me" disruptive />
                        <Button text="Dummy" />
                    </CardActions>
                </Card>
            </div>
        </>
    );
}
