angular.module("wust.elements").directive("smallPost", smallPost);

smallPost.$inject = ["ContextService", "$animate"];

function smallPost(ContextService, $animate) {
    return {
        restrict: "A",
        templateUrl: "elements/post/small_post.html",
        scope: {
            node: "=",
            ignoreTags: "="
        },
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true,
        link: function (scope, elem) {
            scope.currentContexts = ContextService.currentContexts;
            scope.$on("context.changed", () => scope.currentContexts = ContextService.currentContexts);

            scope.$watch("node", function(node) {
                elem[0].classList.add("element_changed");
                console.log("i triggered");
            });
        }
    };
}

smallPostCtrl.$inject = ["Auth"];

function smallPostCtrl(Auth) {
    let vm = this;

    vm.Auth = Auth;
}

