angular.module("wust.elements").directive("showTimestamp", showTimestamp);

showTimestamp.$inject = ["$interval"];

function showTimestamp($interval) {
    return {
        restrict: "A",
        template: "<span ng-bind='ago'></span>",
        scope: {
            node: "="
        },
        replace: true,
        link
    };

    function link(scope) {
        setTimestamp();

        //backoff time depending on how recent it is
        let stopInterval = $interval(setTimestamp, 10 * 1000);

        scope.$on("$destroy", () => $interval.cancel(stopInterval));

        function setTimestamp() {
            let diff = {};
            diff.msec = new Date().getTime() - scope.node.timestamp;
            diff.sec = Math.round(diff.msec / 1000);
            diff.min = Math.round(diff.sec / 60);
            diff.hour = Math.round(diff.min / 60);
            diff.day = Math.round(diff.hour / 24);
            diff.week= Math.round(diff.day / 7);
            diff.month = Math.round(diff.week / 4);
            diff.year = Math.round(diff.month / 12);

            let date = new Date(diff);
            if (diff.sec < 60)
                scope.ago = diff.sec + " seconds ago";
            else if (diff.min < 60)
                scope.ago = diff.min + " minutes ago";
            else if (diff.hour < 24)
                scope.ago = diff.hour + " hours ago";
            else if (diff.day < 7)
                scope.ago = diff.day + " days ago";
            else if (diff.week < 4)
                scope.ago = diff.week + " weeks ago";
            else if (diff.month < 12)
                scope.ago = diff.month + " months ago";
            else
                scope.ago = diff.year + " years ago";
        }
    }
}
