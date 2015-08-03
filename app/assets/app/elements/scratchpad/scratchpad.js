angular.module("wust.components").directive("scratchpad", scratchpad);

scratchpad.$inject = [];

function scratchpad() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "assets/app/elements/scratchpad/scratchpad.html",
        scope: true,
        controller: scratchpadCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

scratchpadCtrl.$inject = ["EditService", "LeftSideService"];

function scratchpadCtrl(EditService, LeftSideService) {
    let vm = this;

    vm.leftSide = LeftSideService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;

    vm.sortableOptions = {
        containment: "#edit_list",
        orderChanged: EditService.persist,
        accept: acceptDrop
    };

    function acceptDrop(sourceItemHandleScope, destSortableScope, destItemScope) {
        // it might happen that someone is dragging an element from another list.
        // this check is only true in our setup, there might be other scopes in between.
        // so if you are here, because the accept function stopped working, it
        // probably is because you added another scope between the
        // sortableScope and itemhandleScope.
        return sourceItemHandleScope.$parent.$parent.$id === destSortableScope.$id;
    }
}
