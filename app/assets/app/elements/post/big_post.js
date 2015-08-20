angular.module("wust.elements").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/big_post.html",
        scope: {
            node: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["SidebarService", "EditService", "Session", "ModalEditService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl(SidebarService, EditService, Session, ModalEditService) {
    let vm = this;

    vm.editNode = EditService.createSession(vm.node);
    vm.replyTo = replyTo;

    function replyTo() {
        ModalEditService.show();
        ModalEditService.currentNode.setReference(vm.editNode);
    }
}
