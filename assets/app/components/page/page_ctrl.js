angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["SidebarService", "EditService", "FullscreenService", "$rootScope", "Auth"];

function PageCtrl(SidebarService, EditService, FullscreenService, $rootScope, Auth) {
    let vm = this;

    vm.sidebar = SidebarService;
    vm.editNode = editNode;
    vm.Auth = Auth;

    function editNode(data) {
        SidebarService.left.visible = true;
        EditService.edit(data, true, 0);
    }

    $rootScope.$on("$stateChangeStart", FullscreenService.hideFullscreens);
}
