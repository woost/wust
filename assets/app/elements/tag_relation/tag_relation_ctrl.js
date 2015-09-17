angular.module("wust.elements").controller("TagRelationCtrl", TagRelationCtrl);

TagRelationCtrl.$inject = ["TagRelationEditService"];

function TagRelationCtrl(TagRelationEditService) {
    let vm = this;

    vm.save = save;
    vm.currentEditRelation = TagRelationEditService.currentRelation;
    vm.disconnect = TagRelationEditService.disconnect;

    function save() {
        vm.currentEditRelation.save();
    }
}
