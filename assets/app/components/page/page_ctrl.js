angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["SidebarService", "EditService", "FullscreenService", "$rootScope"];

function PageCtrl(SidebarService, EditService, FullscreenService, $rootScope) {
    let vm = this;

    vm.sidebar = SidebarService;
    vm.editNode = editNode;

    function editNode(data) {
        SidebarService.left.visible = true;
        EditService.edit(data);
    }

    $rootScope.$on("$stateChangeStart", FullscreenService.hideFullscreens);
}
