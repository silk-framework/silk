import React, { useState } from 'react';
import CreateProjectModal from "./CreateProjectModal";
//import { Button } from "@blueprintjs/core";
import { Button } from "@wrappers/index";

const EmptyWorkspace = () => {
    const [openCreateDialog, setOpenCreateDialog] = useState<boolean>(false);

    const toggleCreateModal = () => {
        setOpenCreateDialog(!openCreateDialog);
    };

    return <>
        <div>
            <p>Workspace is empty, so start please create your first project</p>
            <Button onClick={toggleCreateModal} large>Create Project</Button>
        </div>
        <CreateProjectModal
            isOpen={openCreateDialog}
            onDiscard={toggleCreateModal}
            onConfirm={toggleCreateModal}
        />
    </>
};

export default EmptyWorkspace;
