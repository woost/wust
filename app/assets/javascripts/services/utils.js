app.factory('Utils', function() {
    function removeElementBy(arr, callback) {
        for (var i = arr.length - 1; i >= 0; i--) {
            if (callback(arr[i])) {
                arr.splice(i, 1);
            }
        }
    }

    function removeElement(arr, element) {
        removeElementBy(arr, function(elem) {
            return elem == element;
        });
    }

    return {
        removeElement: removeElement,
        removeElementBy: removeElementBy
    };
});
