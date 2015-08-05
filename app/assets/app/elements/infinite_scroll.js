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
        var atPercent = attrs.atPercent || 80;
        var targetElement = attrs.scrollTarget ? document.getElementById(attrs.scrollTarget) : elem[0];
        var lastScrollTop = 0;

        targetElement.addEventListener("scroll", function () {
            var remainingHeight = targetElement.offsetHeight - targetElement.scrollHeight;
            var scrollTop = targetElement.scrollTop;
            if (scrollTop > lastScrollTop) {
                var percent = Math.abs((scrollTop / remainingHeight) * 100);
                if (percent >= atPercent) {
                    scope.infiniteScroll();
                }
            }

            lastScrollTop = scrollTop;
        });
    }
}
