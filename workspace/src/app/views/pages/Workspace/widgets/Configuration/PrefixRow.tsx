import { IconNames } from "@wrappers/constants";
import React from "react";
import Icon from "@wrappers/icon";
import styles from './index.module.scss';
import { IPrefixState } from "@ducks/workspace/typings";

interface IProps {
    prefix: IPrefixState;
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
