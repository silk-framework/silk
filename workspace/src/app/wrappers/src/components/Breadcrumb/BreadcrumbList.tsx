import React from "react";
// import PropTypes from 'prop-types';
import { Breadcrumbs as BlueprintBreadcrumbList } from "@blueprintjs/core";
//import { BreadcrumbItem, IBreadcrumbItemProps} from "./BreadcrumbItem";
import BreadcrumbItem from "./BreadcrumbItem";
import { IBreadcrumbItemProps } from "./BreadcrumbItem";
import { routerOp } from "@ducks/router";
import { useDispatch } from "react-redux";

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
        char that devides breadcrumb items, default: "/" (currently unsupported)
    */
    itemDivider?: string;
}

function BreadcrumbList({
    className = "",
    // itemDivider = "/",
    ...otherProps
}: IBreadcrumbListProps) {
    const dispatch = useDispatch();

    const gotoPage = (page) => (e) => {
        e.preventDefault();
        if (page) {
            dispatch(routerOp.goToPage(page, {}));
        }
    };

    const renderBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem {...propsBreadcrumb} /*itemDivider="/"*/ onClick={gotoPage(propsBreadcrumb.href)} />;
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
