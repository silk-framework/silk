import React, { useEffect, useState } from 'react';
import './Metadata.scss';
import Card from "@wrappers/blueprint/card";
import { H4 } from "@wrappers/blueprint/typography";
import { sharedOp } from "@ducks/shared";

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
                    <H4>
                        Details & Metadata
                    </H4>
                    <p>
                        Name: {name || id}
                    </p>
                    {
                        description && <p>Description: {description}</p>
                    }
                </Card>
            </div>
        </>
    );
}
