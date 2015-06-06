angular.module("wust.components").directive("sidebar", sidebar);

sidebar.$inject = [];

function sidebar() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/sidebar/sidebar.html",
        scope: true,
        controller: SidebarCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

SidebarCtrl.$inject = ["HistoryService"];

function SidebarCtrl(HistoryService) {
    let vm = this;

    vm.visited = HistoryService.visited;
}
