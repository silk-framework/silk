import React, { useState } from 'react';
import Button from '@wrappers/blueprint/button';
import CreateProjectModal from "./CreateProjectModal";

const EmptyWorkspace = () => {
    const [openCreateDialog, setOpenCreateDialog] = useState<boolean>(false);

    const toggleCreateModal = () => {
        setOpenCreateDialog(!openCreateDialog);
    };

    return <>
        <div>
            <p>Workspace is empty, so start please create your first project</p>
            <Button onClick={toggleCreateModal}>Create Project</Button>
        </div>
        <CreateProjectModal
            isOpen={openCreateDialog}
            onDiscard={toggleCreateModal}
            onConfirm={toggleCreateModal}
        />
    </>
};

export default EmptyWorkspace;
