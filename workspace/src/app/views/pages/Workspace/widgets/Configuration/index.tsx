import React, { useEffect, useState } from "react";
import Card from "../../../../../wrappers/card";
import Button from "../../../../../wrappers/button";
import PrefixesDialog from "./PrefixesDialog";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IPrefixState } from "@ducks/workspace/typings";

const ConfigurationWidget = () => {
    const dispatch = useDispatch();
    const projectId = useSelector(workspaceSel.currentProjectIdSelector);
    const prefixList = useSelector(workspaceSel.prefixListSelector);

    const [visiblePrefixes, setVisiblePrefixes] = useState<IPrefixState[]>([]);
    const [isOpen, setIsOpen] = useState<boolean>(false);

    const VISIBLE_COUNT = 5;

    useEffect(() => {
        getPrefixesList();
    }, [projectId]);

    useEffect(() => {
        const visibleItems = prefixList.slice(0, VISIBLE_COUNT);
        setVisiblePrefixes(visibleItems);
    }, [prefixList]);

    const getFullSizeOfList = () => Object.keys(prefixList).length;
    const handleOpen = () => setIsOpen(true);
    const handleClose = () => setIsOpen(false);

    const getPrefixesList = () => {
        dispatch(workspaceOp.fetchProjectPrefixesAsync());
    };

    const optionalMore = () => { // Displays how many more prefixes are defined that are not displayed in preview
        return getFullSizeOfList() - VISIBLE_COUNT > 0 ? <b> and {getFullSizeOfList() - VISIBLE_COUNT} more</b> : '';
    };
    return (
        <Card>
            <h3>Configuration</h3>
            <div>
                <p><strong>Prefix Settings ({(getFullSizeOfList())})</strong></p>
                {
                    visiblePrefixes.map((o, index) =>
                        <span key={index}>
                            {o.prefixName}
                            {
                                index < visiblePrefixes.length - 1
                                    ? ', '
                                    : optionalMore()
                            }
                        </span>
                    )
                }
            </div>
            <Button onClick={handleOpen}>Change Prefix Settings</Button>
            {
                <PrefixesDialog
                    isOpen={isOpen}
                    onCloseModal={handleClose}
                />
            }
        </Card>
    )
};

export default ConfigurationWidget;
