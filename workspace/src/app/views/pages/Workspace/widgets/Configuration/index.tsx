import React, { useEffect, useState } from "react";
import Card from "../../../../../wrappers/card";
import Button from "../../../../../wrappers/button";
import { sharedOp } from "../../../../../store/ducks/shared";
import PrefixesDialog from "./PrefixesDialog";

interface IProps {
    projectId: string;
}

const ConfigurationWidget = ({ projectId }: IProps) => {
    const [prefixList, setPrefixList] = useState<Object>({});
    const [visiblePrefixes, setVisiblePrefixes] = useState<string[]>([]);
    const [isOpen, setIsOpen] = useState<boolean>(false);

    const VISIBLE_COUNT = 5;

    useEffect(() => {
        getPrefixesList(projectId)
    }, [projectId]);

    const getPrefixesList = async (projectId) => {
        try {
            const data = await sharedOp.getProjectPrefixes(projectId);
            const arr = Object.keys(data).slice(0, VISIBLE_COUNT);
            setVisiblePrefixes(arr);
            setPrefixList(data);
        } catch {}
    };

    const getFullSizeOfList = () => Object.keys(prefixList).length;
    const handleOpen = () => setIsOpen(true);
    const handleClose = () => setIsOpen(false);

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
                />
            }
        </Card>
    )
};

export default ConfigurationWidget;
