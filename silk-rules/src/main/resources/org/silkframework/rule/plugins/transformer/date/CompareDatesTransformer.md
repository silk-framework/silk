Compares two dates.
Returns 1 if the comparison yields true and 0 otherwise.
If there are multiple dates in both sets, the comparator must be true for all dates.
For instance, {2014-08-02,2014-08-03} < {2014-08-03} yields 0 as not all dates in the first set are smaller than in the second.