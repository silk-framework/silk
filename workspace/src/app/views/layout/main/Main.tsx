import React from 'react';
import './Main.scss';

const Main = ({children, ...restProps}) => {
    return (
        <section className='main-content clearfix' {...restProps}>
            {children}
        </section>
    )
};

const _LeftPanel = ({children, className, ...restProps}) => {
    return (
        <div className={`left-side-content ${className}`} {...restProps}>
            {children}
        </div>
    )
};

const _RightPanel = ({children, className,...restProps}) => {
    return (
        <div className={`right-side-widgets ${className}`} {...restProps}>
            {children}
        </div>
    )

};

Main.LeftPanel = _LeftPanel;
Main.RightPanel = _RightPanel;

export default Main;
