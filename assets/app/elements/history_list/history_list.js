angular.module("wust.elements").directive("historyList", historyList);

historyList.$inject = [];

function historyList() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/history_list/history_list.html",
        scope: true,
        controller: HistoryListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

HistoryListCtrl.$inject = ["HistoryService"];

function HistoryListCtrl(HistoryService) {
    let vm = this;

    vm.history = HistoryService;
}
