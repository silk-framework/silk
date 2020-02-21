import React from 'react';
import styles from './index.module.scss';
import Grid from "@wrappers/carbon/grid";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";

const Main = ({children, ...restProps}) => {
    return (
        <Grid className={styles.mainContent} {...restProps}>
            <Row>
                {children}
            </Row>
        </Grid>
    )
};

const _LeftPanel = ({children, span = 0, className = '', ...restProps}) => {
    return (
        <Col span={10} className={`left-side-content ${className}`} {...restProps}>
            {children}
        </Col>
    )
};

const _RightPanel = ({children, span = 0, className = '',...restProps}) => {
    return (
        <Col span={2} className={`right-side-widgets ${className}`} {...restProps}>
            {children}
        </Col>
    )

};

Main.LeftPanel = _LeftPanel;
Main.RightPanel = _RightPanel;

export default Main;
