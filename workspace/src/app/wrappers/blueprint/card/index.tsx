import React from 'react';
import { Card as B_Card } from "@blueprintjs/core";

const Card = ({children, ...restProps}: any) => <B_Card {...restProps}>{children}</B_Card>;

export default Card;
