angular.module("wust.services").value("UniqArr", UniqArr);

function UniqArr(uniqProperties, initialArray = []) {
    let arr = initialArray;

    arr.push = function(...elems) {
        _.each(elems, elem => {
            if (!_.any(arr, _.pick(elem, uniqProperties)))
                Array.prototype.push.apply(arr, [elem]);
        });

        return arr.length;
    };

    arr.remove = function(...elems) {
        _.each(elems, elem => {
            _.remove(arr, _.pick(elem, uniqProperties));
        });

        return arr.length;
    };

    return arr;
}

function UniqArrRelation(arr = []) {
    return UniqArr(["startId", "endId"], arr);
}

function UniqArrNode(arr = []) {
    return UniqArr(["id"], arr);
}

UniqArr.relation = UniqArrRelation;
UniqArr.node = UniqArrNode;
