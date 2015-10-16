angular.module("wust.elements").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "elements/tag/big_taglist.html",
        scope: {
            node: "="
        },
        controller: bigTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigTaglistCtrl.$inject = ["Post", "ContextService", "Auth"];

function bigTaglistCtrl(Post, ContextService, Auth) {
    let vm = this;

    vm.Auth = Auth;
    vm.upvoteTag = upvoteTag;

    //TODO: semnatic downvote on post
    function upvoteTag(tag) {
        let service = Post.$buildRaw(_.pick(vm.node, "id")).tags.$buildRaw(_.pick(tag, "id"));
        if (tag.vote) {
            service.neutral.$create().$then(data => {
                tag.vote = undefined;
                tag.quality = data.quality;
                // humane.success("Unvoted post in context");
                // ContextService.setNodeContext(vm.node);
            }, resp => humane.error(resp.$response.data));
        } else {
            service.up.$create().$then(data => {
                tag.vote = data.vote;
                tag.quality = data.quality;
                // humane.success("Upvoted post in context");
                // ContextService.setNodeContext(vm.node);
            }, resp => humane.error(resp.$response.data));
        }
    }

}
