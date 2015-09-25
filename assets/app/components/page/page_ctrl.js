angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["SidebarService", "EditService", "FullscreenService", "$rootScope", "Auth"];

function PageCtrl(SidebarService, EditService, FullscreenService, $rootScope, Auth) {
    let vm = this;

    //TODO: allow drag/drop only for Auth.isLoggedIn

    vm.sidebar = SidebarService;
    vm.editNode = editNode;
    vm.Auth = Auth;

    function editNode(data) {
        SidebarService.left.visible = true;
        EditService.edit(data, 0, true);
    }

    $rootScope.$on("$stateChangeStart", FullscreenService.hideFullscreens);
}
