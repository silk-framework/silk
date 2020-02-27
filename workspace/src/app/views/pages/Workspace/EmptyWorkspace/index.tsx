import React, { useState } from 'react';
import Button from '@wrappers/blueprint/button';
import CreateProjectModal from "../../../components/modals/CreateProjectModal";

const EmptyWorkspace = () => {
    const [openCreateDialog, setOpenCreateDialog] = useState<boolean>(false);

    const toggleCreateModal = () => {
        setOpenCreateDialog(!openCreateDialog);
    };

    const handleConfirmCreation = (formData: any) => {
        console.log(formData);
    };

    return <>
        <div>
            <p>Workspace is empty, so start please create your first project</p>
            <Button onClick={toggleCreateModal}>Create Project</Button>
        </div>
        <CreateProjectModal
            isOpen={openCreateDialog}
            onDiscard={toggleCreateModal}
            onConfirm={handleConfirmCreation}
        />
    </>
};

export default EmptyWorkspace;
