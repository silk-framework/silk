import React from 'react';
import './Main.scss';

const Main = ({children, ...restProps}) => {
    return (
        <section className='main-content clearfix' {...restProps}>
            {children}
        </section>
    )
};

const _LeftSide = ({children, className, ...restProps}) => {
    return (
        <div className={`left-side-content ${className}`} {...restProps}>
            {children}
        </div>
    )
};

const _RightSide = ({children, className,...restProps}) => {
    return (
        <div className={`right-side-widgets ${className}`} {...restProps}>
            {children}
        </div>
    )

};

Main.LeftPanel = _LeftSide;
Main.RightPanel = _RightSide;

export default Main;
