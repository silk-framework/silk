import React, { useEffect, useState } from "react";
import Card from "../../../../../wrappers/card";
import Button from "../../../../../wrappers/button";
import { sharedOp } from "../../../../../store/ducks/shared";
import PrefixesDialog from "./PrefixesDialog";

interface IProps {
    projectId: string;
}

const ConfigurationWidget = ({projectId}: IProps) => {
    const [prefixList, setPrefixList] = useState<Object>({});
    const [visiblePrefixes, setVisiblePrefixes] = useState<string[]>([]);
    const [isOpen, setIsOpen] = useState<boolean>(false);

    const VISIBLE_COUNT = 5;

    useEffect(() => {
        getPrefixesList();
    }, [projectId]);

    const getFullSizeOfList = () => Object.keys(prefixList).length;
    const handleOpen = () => setIsOpen(true);
    const handleClose = () => setIsOpen(false);

    const updateThePrefixList = (data) => {
        const arr = Object.keys(data).slice(0, VISIBLE_COUNT);
        setVisiblePrefixes(arr);
        setPrefixList(data);
    };

    const getPrefixesList = async () => {
        const data = await sharedOp.getProjectPrefixes(projectId);
        updateThePrefixList(data);
    };

    const handleRemovePrefix = async (prefixName: string) => {
        const data = await sharedOp.removeProjectPrefixes(projectId, prefixName);
        updateThePrefixList(data);
    };

    const handleAddOrUpdatePrefix = async (prefixName: string, prefixUri: string) => {
        const data = await sharedOp.addProjectPrefix(projectId, prefixName, prefixUri)
        updateThePrefixList(data);
    };

    return (
        <Card>
            <h3>Configuration</h3>
            <div>
                <p><strong>Prefix Settings ({(getFullSizeOfList())})</strong></p>
                {
                    visiblePrefixes.map((key, index) =>
                        <span key={key}>
                            {key}
                            {
                                index < visiblePrefixes.length - 1
                                    ? ', '
                                    : <b> and {getFullSizeOfList() - VISIBLE_COUNT} more</b>
                            }
                        </span>
                    )
                }
            </div>
            <Button onClick={handleOpen}>Add Prefix Settings</Button>
            {
                isOpen && <PrefixesDialog
                    prefixList={prefixList}
                    onCloseModal={handleClose}
                    onRemove={handleRemovePrefix}
                    onAddOrUpdate={handleAddOrUpdatePrefix}
                />
            }
        </Card>
    )
};

export default ConfigurationWidget;
