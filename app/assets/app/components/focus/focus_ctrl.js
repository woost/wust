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

            // fire window.resize event to recalculate d3 graph node dimensions.
            // they are 0x0 in some cases.
            setTimeout( () => {
                var evt = document.createEvent("UIEvents");
                evt.initUIEvent("resize", true, false,window,0);
                window.dispatchEvent(evt);
            }, 200 );
        }
    }

    vm.graphComponent = component.wrap("graph");
    vm.neighboursComponent = component.hyperWrap("focus");
    // vm.branchesComponent = component.wrap();

    vm.tabViews = _.map([0, 1, 2, 3], i => new Tab(i));
    vm.tabViews[HistoryService.activeViewIndex]._active = true;

    // we are viewing details about a node, so add it to the nodehistory
    HistoryService.add(vm.neighboursComponent.rootNode);
    HistoryService.currentViewComponent = component;
}
