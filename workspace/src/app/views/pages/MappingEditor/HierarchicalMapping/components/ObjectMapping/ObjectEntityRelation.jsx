import React from "react";
import { RadioButton, FieldItem } from "@eccenca/gui-elements";
import { ParentElement } from "../ParentElement";

const ObjectEntityRelation = ({ isBackwardProperty, parent }) => {
    return (
        <FieldItem>
            <RadioButton
                disabled
                name="from"
                checked={!isBackwardProperty}
                value="from"
                label={
                    <div>
                        Connect from <ParentElement parent={parent} />
                    </div>
                }
            />
            <RadioButton
                disabled
                name="to"
                checked={isBackwardProperty}
                value="to"
                label={
                    <div>
                        Connect to <ParentElement parent={parent} />
                    </div>
                }
            />
        </FieldItem>
    );
};

export default ObjectEntityRelation;
