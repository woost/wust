angular.module("wust.elements").directive("historyList", historyList);

historyList.$inject = [];

function historyList() {
    return {
        restrict: "A",
        templateUrl: "history_list/history_list.html",
        scope: true,
        controller: HistoryListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

HistoryListCtrl.$inject = ["HistoryService"];

function HistoryListCtrl(HistoryService) {
    let vm = this;

    vm.visited = HistoryService.visited;
}
