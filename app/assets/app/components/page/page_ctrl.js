angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["LeftSideService", "EditService"];

function PageCtrl(LeftSideService, EditService) {
    let vm = this;

    vm.leftSide = LeftSideService;
    vm.editNode = editNode;

    function editNode(data) {
        LeftSideService.visible = true;
        EditService.edit(data);
    }
}
