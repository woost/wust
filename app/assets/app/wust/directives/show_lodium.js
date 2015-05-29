angular.module("wust").directive("showLodium", showLodium);

showLodium.$inject = [];

function showLodium() {
    return {
        restrict: "EA",
        scope: {
            enabled: "="
        },
        link: link
    };

    function link(scope, element) {
        let lodium = new Lodium(element[0]);
        scope.$watch("enabled", enabled => {
            if (enabled)
                lodium.start();
            else
                lodium.stop();
        });
    }
}
