import React from "react";
import { Icon, PropertyValuePair, PropertyName, PropertyValue } from "@eccenca/gui-elements";
import { ParentElement } from "../ParentElement";

const ObjectEntityRelation = ({ isBackwardProperty, parent }) => {
    return (
        <PropertyValuePair singleColumn>
            <PropertyName labelProps={{ emphasis: "strong" }}>Property direction</PropertyName>
            <PropertyValue>
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
            </PropertyValue>
        </PropertyValuePair>
    );
};

export default ObjectEntityRelation;
