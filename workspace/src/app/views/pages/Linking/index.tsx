import React, { useEffect, useState } from "react";

import './index.scss';
import { useDispatch, useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";
import { useParams } from "react-router";
import Main from "../../layout/Main";
import Metadata from "../../components/Metadata";
import { sharedOp } from "@ducks/shared";
import { datasetOp, datasetSel } from "@ducks/dataset";

export default function() {
    const error = useSelector(datasetSel.errorSelector);
    const {linkingId, projectId} = useParams();
    const [metadata, setMetadata] = useState({});
    const dispatch = useDispatch();

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    useEffect(() => {
        getTaskMetadata(linkingId, projectId);
    }, [linkingId, projectId]);

    const getTaskMetadata = async(linkingId: string, projectId: string) => {
        dispatch(datasetOp.setLoading(true));
        const data = await sharedOp.getTaskMetadataAsync(linkingId, projectId);
        setMetadata(data);
        dispatch(datasetOp.setLoading(false));
    };

    return (
        <Main>
            <Main.LeftPanel>
                <Metadata metadata={metadata}/>
            </Main.LeftPanel>
        </Main>
    );
}
