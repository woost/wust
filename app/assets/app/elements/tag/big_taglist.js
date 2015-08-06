angular.module("wust.elements").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/tag/big_taglist.html",
        scope: {
            node: "="
        },
        controller: bigTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigTaglistCtrl.$inject = ["Post", "DiscourseNode"];

function bigTaglistCtrl(Post, DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.TagLike;

    vm.upvote = upvote;
    vm.downvote = downvote;

    function wrapResource(tag) {
        let model = Post.$buildRaw(_.pick(vm.node, "id"));
        return model.tags.$buildRaw(tag).$reveal();
    }

    function upvote(tag) {
        let resource = wrapResource(tag);
        resource.up.$create().$then(() => humane.success("Up voted"));
    }
    function downvote(tag) {
        let resource = wrapResource(tag);
        resource.down.$create().$then(() => humane.success("Down voted"));
    }
}
