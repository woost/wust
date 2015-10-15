angular.module("wust.elements").directive("smallPost", smallPost);

smallPost.$inject = ["ContextService"];

function smallPost(ContextService) {
    return {
        restrict: "A",
        templateUrl: "elements/post/small_post.html",
        scope: {
            node: "=",
            ignoreTags: "="
        },
        transclude: true,
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true,
        link: function (scope, elem) {
            scope.currentContexts = ContextService.currentContexts;
            scope.$on("context.changed", () => scope.currentContexts = ContextService.currentContexts);
        }
    };
}

smallPostCtrl.$inject = ["Auth"];

function smallPostCtrl(Auth) {
    let vm = this;

    vm.Auth = Auth;
}

