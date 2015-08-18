angular.module("wust.elements").controller("TagRelationCtrl", TagRelationCtrl);

TagRelationCtrl.$inject = ["TagRelationEditService"];

function TagRelationCtrl(TagRelationEditService) {
    let vm = this;

    vm.save = save;
    vm.currentEditRelation = TagRelationEditService.currentRelation;

    function save() {
        console.log(vm.currentEditRelation, vm.selectedTags);
        vm.currentEditRelation.save();
    }
}
