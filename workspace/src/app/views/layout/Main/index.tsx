import React from 'react';
import styles from './index.module.scss';

const Main = ({children}) => {
    return (
        <div className={styles.mainContent}>
            {children}
        </div>
    )
};

export default Main;
