angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["SidebarService", "EditService"];

function PageCtrl(SidebarService, EditService) {
    let vm = this;

    vm.sidebar = SidebarService;
    vm.editNode = editNode;

    function editNode(data) {
        SidebarService.visible = true;
        EditService.edit(data);
    }
}
