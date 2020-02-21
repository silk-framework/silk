import React, { useEffect } from "react";

import './index.scss';
import { useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/blueprint/constants";
import { useParams } from "react-router";
import Main from "../../layout/Main";
import Metadata from "../../components/Metadata";
import { datasetSel } from "@ducks/dataset";

export default function () {
    const error = useSelector(datasetSel.errorSelector);
    const {linkingId, projectId} = useParams();

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    return (
        <Main>
            <>
                <Metadata projectId={projectId} taskId={linkingId}/>
            </>
        </Main>
    );
}
