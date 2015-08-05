angular.module("wust.elements").directive("infiniteScroll", infiniteScroll);

infiniteScroll.$inject = [];

function infiniteScroll() {
    return {
        scope: {
            infiniteScroll: "&"
        },
        link
    };

    function link(scope, elem, attrs) {
        var rawElem = elem[0];
        var atPercent = attrs.atPercent || 80;

        elem.bind("scroll", function () {
            var remainingHeight = rawElem.offsetHeight - rawElem.scrollHeight;
            var scrollTop = rawElem.scrollTop;
            var percent = Math.abs((scrollTop / remainingHeight) * 100);
            if (percent >= atPercent) {
                scope.infiniteScroll();
            }
        });
    }
}
