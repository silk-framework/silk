import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { ParentElement } from "../ParentElement";

const ObjectEntityRelation = ({ isBackwardProperty, parent }) => {
    return (
        <div data-test-id={"object-entity-relation"}>
            {isBackwardProperty ? (
                <>
                    <Icon name={"navigation-left"} small />
                    &nbsp; Connect to <ParentElement parent={parent} />
                </>
            ) : (
                <>
                    <Icon name={"navigation-right"} small />
                    &nbsp; Connect from <ParentElement parent={parent} />
                </>
            )}
        </div>
    );
};

export default ObjectEntityRelation;
