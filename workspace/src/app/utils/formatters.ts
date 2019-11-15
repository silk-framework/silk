import { prefixes } from 'ecc-queryutils';
import moment from "moment";
const formatter = new Map();

const formatDatetime = ({ value }) => {
    return moment(value).fromNow();
};

formatter.set(`${prefixes.xsd}dateTime`, formatDatetime);

export default formatter;
