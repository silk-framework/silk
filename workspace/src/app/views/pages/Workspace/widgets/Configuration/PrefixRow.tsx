import { IconNames } from "@wrappers/constants";
import React from "react";
import Icon from "@wrappers/icon";
import { IFormattedPrefix } from "./PrefixesDialog";
import styles from './index.module.scss';

interface IProps {
    prefix: IFormattedPrefix;
    onRemove();
}

const PrefixRow = ({prefix, onRemove}: IProps) => {
    return (
        <div className={`${styles.prefixRow} row`}>
            <div className="col-5">
                <span>{prefix.prefixName}</span>
            </div>
            <div className="col-6">
                <span>{prefix.prefixUri}</span>
            </div>
            <div className="col-1">
                <Icon
                    icon={IconNames.TRASH}
                    onClick={onRemove}
                />
            </div>
        </div>
    )
};

export default PrefixRow;
