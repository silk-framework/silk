import React, { useEffect } from "react";

import './index.scss';
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";
import { useParams } from "react-router";

export default function() {
    const error = useSelector(workspaceSel.errorSelector);
    const {datasetId} = useParams();

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    return null;
}
