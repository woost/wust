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

bigTaglistCtrl.$inject = ["Post", "Session"];

function bigTaglistCtrl(Post, Session) {
    let vm = this;

    vm.upvote = upvote;
    vm.downvote = downvote;
    vm.getVote = getVote;

    let tagCache = {};

    function wrapResource(tag) {
        let model = Post.$buildRaw(_.pick(vm.node, "id"));
        return model.tags.$buildRaw(tag).$reveal();
    }

    function getVote(tag) {
        if (tagCache[tag.id] === undefined)
            tagCache[tag.id] = Session.getVote(tag.id, vm.node.id);

        return tagCache[tag.id];
    }

    function upvote(tag) {
        let resource = wrapResource(tag);
        resource.up.$create().$then(val => {
            Session.addVote(tag.id, vm.node.id, val);
            humane.success("Up voted");
        });
    }
    function downvote(tag) {
        let resource = wrapResource(tag);
        resource.down.$create().$then(val => {
            Session.addVote(tag.id, vm.node.id, val);
            humane.success("Down voted");
        });
    }
}
