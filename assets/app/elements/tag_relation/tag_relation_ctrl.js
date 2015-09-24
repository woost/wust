angular.module("wust.elements").controller("TagRelationCtrl", TagRelationCtrl);

TagRelationCtrl.$inject = ["TagRelationEditService"];

function TagRelationCtrl(TagRelationEditService) {
    let vm = this;

    vm.save = save;
    vm.cancel = cancel;
    vm.currentEditRelation = TagRelationEditService.currentRelation;
    vm.disconnect = TagRelationEditService.disconnect;

    function cancel() {
        if( vm.currentEditRelation.isCreating )
            vm.disconnect();
    }

    function save() {
        vm.currentEditRelation.save();
    }
}
