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

        //TODO: backoff time depending on how recent it is
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
                scope.ago = agoString(diff.sec, "second");
            else if (diff.min < 60)
                scope.ago = agoString(diff.min, "minute");
            else if (diff.hour < 24)
                scope.ago = agoString(diff.hour, "hour");
            else if (diff.day < 7)
                scope.ago = agoString(diff.day, "day");
            else if (diff.week < 4)
                scope.ago = agoString(diff.week, "week");
            else if (diff.month < 12)
                scope.ago = agoString(diff.month, "month");
            else
                scope.ago = agoString(diff.year, "year");

            function agoString(num, unit) {
                let sunit = num === 1 ? unit : unit + "s";
                return `${num} ${sunit} ago`;
            }
        }
    }
}
