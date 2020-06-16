import React from "react";
import { Breadcrumbs as BlueprintBreadcrumbList } from "@blueprintjs/core";
import BreadcrumbItem from "./BreadcrumbItem";
import { IBreadcrumbItemProps } from "./BreadcrumbItem";

interface IBreadcrumbListProps extends React.HTMLAttributes<HTMLUListElement> {
    /**
        space-delimited list of class names
    */
    className?: string;
    /**
        list of breadcrumb items to display
    */
    items: IBreadcrumbItemProps[];
    /**
        click handler used on breadcrumb items
    */
    onItemClick?(itemUrl: string, event: object): any;
    /**
        char that devides breadcrumb items, default: "/" (currently unsupported)
    */
    itemDivider?: string;
}

function BreadcrumbList({
    className = "",
    // itemDivider = "/",
    onItemClick,
    ...otherProps
}: IBreadcrumbListProps) {
    const renderBreadcrumb = (propsBreadcrumb) => {
        return (
            <BreadcrumbItem
                {...propsBreadcrumb}
                /*itemDivider="/"*/ onClick={
                    onItemClick
                        ? (e) => {
                              onItemClick(propsBreadcrumb.href, e);
                          }
                        : undefined
                }
            />
        );
    };

    const renderCurrentBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem {...propsBreadcrumb} current={true} href={null} /*itemDivider={itemDivider}*/ />;
    };

    return (
        <BlueprintBreadcrumbList
            {...otherProps}
            className={"ecc-breadcrumb__list " + className}
            minVisibleItems={1}
            breadcrumbRenderer={renderBreadcrumb}
            currentBreadcrumbRenderer={renderCurrentBreadcrumb}
        />
    );
}

export default BreadcrumbList;
