angular.module("wust.components").controller("ContentCtrl", ContentCtrl);

ContentCtrl.$inject = ["Post", "DiscourseNode", "Scratchpad"];

function ContentCtrl(Post, DiscourseNode, Scratchpad) {
    let vm = this;

    vm.scratchpad = Scratchpad.settings;

    vm.adder = {
        info: DiscourseNode.Post,
        addNode: addNode,
        newNode: Post.$build({
            title: ""
        })
    };

    function addNode() {
        angular.copy(this.newNode).$save().$then(data => {
            humane.success("Added new node");
            this.info.gotoState(data.id);
        });
    }
}
