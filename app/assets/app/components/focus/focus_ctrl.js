angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["$stateParams", "HistoryService", "component"];

function FocusCtrl($stateParams, HistoryService, component) {
    let vm = this;

    class Tab {
        constructor(index) {
            this.index = index;
        }
        get active() {
            return this._active;
        }
        set active(active) {
            this._active = active;
            if (active)
                HistoryService.changeActiveView(this.index);
        }
    }

    vm.rootId = $stateParams.id;
    vm.component = component;
    vm.tabViews = _.map([0, 1, 2, 3], i => new Tab(i));
    vm.tabViews[HistoryService.activeViewIndex]._active = true;
}
